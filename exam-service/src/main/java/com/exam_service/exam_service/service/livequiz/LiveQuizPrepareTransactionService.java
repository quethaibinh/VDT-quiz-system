package com.exam_service.exam_service.service.livequiz;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionSnapshotItem;
import com.exam_service.exam_service.client.QuestionSnapshotOption;
import com.exam_service.exam_service.model.dto.cache.AnswerEntryDTO;
import com.exam_service.exam_service.model.dto.cache.PaperOptionDTO;
import com.exam_service.exam_service.model.dto.cache.PaperQuestionDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.model.entity.enums.QuestionDifficulty;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
/**
 * Transaction ngan cho prepare live quiz: chi ghi DB, khong goi Runtime trong transaction.
 *
 * Lop nay chiu trach nhiem bien ban nhap DRAFT thanh PREPARED bang cach ghi
 * ExamQuestion snapshot. Neu transaction rollback thi Runtime chua duoc goi, vi
 * vay khong co phong mo coi.
 */
public class LiveQuizPrepareTransactionService {

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final ObjectMapper objectMapper;

    public LiveQuizPrepareTransactionService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            ObjectMapper objectMapper
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Exam prepare(
            UUID subjectId,
            UUID quizId,
            UUID teacherId,
            long expectedVersion,
            QuestionCollectionSnapshot snapshot
    ) {
        // Lock theo row exam de hai request prepare cung luc khong tao trung snapshot.
        Exam quiz = examRepo.findOwnedForUpdate(quizId, subjectId, teacherId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_NOT_FOUND"));
        // Live quiz dung chung bang Exam nen phai chan scheduled exam ngay trong transaction.
        if (quiz.getExamType() != ExamType.LIVE_QUIZ) {
            throw new NotFoundException("LIVE_QUIZ_NOT_FOUND");
        }
        if (quiz.getStatus() == ExamStatus.PREPARED && questionRepo.existsByExamId(quizId)) {
            // Cho phep retry prepare sau loi Runtime: DB da san sang thi tra lai exam hien co.
            return quiz;
        }
        if (quiz.getStatus() != ExamStatus.DRAFT) {
            throw new ConflictException("LIVE_QUIZ_NOT_EDITABLE");
        }
        if (quiz.getVersion() != expectedVersion) {
            // Version lay truoc khi goi Question Service; neu user sua quiz giua chung thi huy snapshot cu.
            throw new ConflictException("LIVE_QUIZ_CHANGED_DURING_PREPARE");
        }
        if (!quiz.getCollectionId().equals(snapshot.collectionId())
                || !quiz.getSubjectId().equals(snapshot.subjectId())) {
            throw new ConflictException("LIVE_QUIZ_BLUEPRINT_CHANGED");
        }
        if (questionRepo.existsByExamId(quizId)) {
            // Bao ve them truong hop da co row cau hoi nhung status chua khop do su co truoc do.
            throw new ConflictException("LIVE_QUIZ_SNAPSHOT_ALREADY_EXISTS");
        }
        validateSnapshot(snapshot.questions());

        List<ExamQuestion> rows = new ArrayList<>();
        int order = 0;
        for (QuestionSnapshotItem item : snapshot.questions()) {
            ExamQuestion row = new ExamQuestion();
            row.setExamId(quizId);
            row.setQuestionId(item.questionId());
            row.setQuestionVersion(Math.toIntExact(item.questionVersion()));
            row.setDifficulty(QuestionDifficulty.valueOf(item.difficulty()));
            row.setScore(item.defaultScore().floatValue());
            row.setTimeLimitSeconds(item.estimatedSecond());
            row.setSortOrder(order++);
            row.setRequired(true);
            // questionSnapshot khong chua dap an dung; hoc sinh/runtime chi can noi dung de hien thi.
            row.setQuestionSnapshot(toJson(toPaperQuestion(item)));
            // answerKeySnapshot la du lieu rieng cho cham diem/ket qua, khong dua vao payload cau hoi.
            row.setAnswerKeySnapshot(toJson(toAnswer(item)));
            rows.add(row);
        }
        questionRepo.saveAll(rows);

        quiz.setStatus(ExamStatus.PREPARED);
        // Tam dung scheduledAt nhu moc preparedAt cho MVP vi schema hien chua co cot preparedAt rieng.
        quiz.setScheduledAt(OffsetDateTime.now());
        quiz.setSnapshotVersion(1);
        return examRepo.save(quiz);
    }

    private void validateSnapshot(List<QuestionSnapshotItem> questions) {
        // Defense in depth: Question Service da build snapshot, nhung Exam Service van tu bao ve invariant.
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("LIVE_QUIZ_SNAPSHOT_EMPTY");
        }
        Set<UUID> questionIds = new HashSet<>();
        for (QuestionSnapshotItem item : questions) {
            if (!questionIds.add(item.questionId())) {
                throw new IllegalArgumentException("DUPLICATE_SNAPSHOT_QUESTION");
            }
            if (item.defaultScore() == null || item.defaultScore() <= 0) {
                throw new IllegalArgumentException("INVALID_EXAM_QUESTION_SCORE");
            }
            if (item.estimatedSecond() == null || item.estimatedSecond() <= 0) {
                throw new IllegalArgumentException("INVALID_EXAM_QUESTION_TIME_LIMIT");
            }
            QuestionDifficulty.valueOf(item.difficulty());
        }
    }

    private PaperQuestionDTO toPaperQuestion(QuestionSnapshotItem item) {
        // DTO nay la ban de render cau hoi, co option nhung khong co flag correct.
        return new PaperQuestionDTO(
                item.questionId(),
                item.questionVersion(),
                item.difficulty(),
                item.type(),
                item.content(),
                item.contentFormat(),
                item.defaultScore(),
                item.estimatedSecond(),
                item.options().stream()
                        .map(option -> new PaperOptionDTO(
                                option.optionId(),
                                option.key(),
                                option.content(),
                                option.contentFormat()
                        ))
                        .toList()
        );
    }

    private AnswerEntryDTO toAnswer(QuestionSnapshotItem item) {
        // DTO nay tach rieng danh sach option dung de tranh lo dap an khi phat cau hoi.
        return new AnswerEntryDTO(
                item.questionId(),
                item.options().stream()
                        .filter(QuestionSnapshotOption::correct)
                        .map(QuestionSnapshotOption::optionId)
                        .toList(),
                item.defaultScore()
        );
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("SNAPSHOT_SERIALIZATION_FAILED", exception);
        }
    }
}
