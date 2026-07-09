package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionSnapshotItem;
import com.exam_service.exam_service.model.dto.cache.AnswerEntryDTO;
import com.exam_service.exam_service.model.dto.cache.PaperOptionDTO;
import com.exam_service.exam_service.model.dto.cache.PaperQuestionDTO;
import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import com.exam_service.exam_service.model.entity.enums.QuestionDifficulty;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.repository.OutboxEventRepo;
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
 * Giu transaction ngan va atomic cho snapshot, lifecycle va outbox.
 * Khong goi service ngoai trong class nay de tranh giu DB lock qua lau.
 */
public class ExamSchedulingTransactionService {

    public static final String CACHE_EVENT = "EXAM_SNAPSHOT_CACHE_REQUESTED";

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final ExamAssignmentRepo assignmentRepo;
    private final OutboxEventRepo outboxRepo;
    private final ObjectMapper objectMapper;

    public ExamSchedulingTransactionService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            ExamAssignmentRepo assignmentRepo,
            OutboxEventRepo outboxRepo,
            ObjectMapper objectMapper
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.assignmentRepo = assignmentRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Exam schedule(
            UUID subjectId,
            UUID examId,
            UUID teacherId,
            long expectedVersion,
            QuestionCollectionSnapshot snapshot
    ) {
        Exam exam = examRepo.findOwnedForUpdate(examId, subjectId, teacherId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND"));
        if (exam.getStatus() == ExamStatus.SCHEDULED && questionRepo.existsByExamId(examId)) {
            return exam;
        }
        if (exam.getExamType() != ExamType.STANDARD_EXAM) {
            throw new ConflictException("LIVE_QUIZ_CANNOT_USE_EXAM_SCHEDULE");
        }
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new ConflictException("EXAM_NOT_EDITABLE");
        }
        if (exam.getVersion() != expectedVersion) {
            throw new ConflictException("EXAM_CHANGED_DURING_SCHEDULING");
        }
        if (!exam.getStartAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("EXAM_START_MUST_BE_IN_FUTURE");
        }
        if (assignmentRepo.countByExamIdAndStatus(examId, AssignmentStatus.ASSIGNED) < 1) {
            throw new ConflictException("EXAM_ASSIGNMENT_REQUIRED");
        }
        if (!exam.getCollectionId().equals(snapshot.collectionId())
                || !exam.getSubjectId().equals(snapshot.subjectId())) {
            throw new ConflictException("EXAM_BLUEPRINT_CHANGED");
        }
        if (questionRepo.existsByExamId(examId)) {
            throw new ConflictException("EXAM_SNAPSHOT_ALREADY_EXISTS");
        }

        validateQuota(exam, snapshot.questions());
        List<ExamQuestion> rows = new ArrayList<>();
        int order = 0;
        for (QuestionSnapshotItem item : snapshot.questions()) {
            ExamQuestion row = new ExamQuestion();
            row.setExamId(examId);
            row.setQuestionId(item.questionId());
            row.setQuestionVersion(Math.toIntExact(item.questionVersion()));
            row.setDifficulty(QuestionDifficulty.valueOf(item.difficulty()));
            row.setScore(item.defaultScore().floatValue());
            row.setTimeLimitSeconds(item.estimatedSecond());
            row.setSortOrder(order++);
            row.setRequired(true);
            row.setQuestionSnapshot(toJson(toPaperQuestion(item)));
            row.setAnswerKeySnapshot(toJson(toAnswer(item)));
            rows.add(row);
        }
        // luu snapshot cau hoi
        questionRepo.saveAll(rows);

        exam.setStatus(ExamStatus.SCHEDULED);
        exam.setScheduledAt(OffsetDateTime.now());
        exam.setSnapshotVersion(1);
        examRepo.save(exam); // cap nhat trang thai

        // tien hanh luu outbox sau khi cap nhat trang thai ca thi
        ExamSnapshotCacheRequested eventPayload = new ExamSnapshotCacheRequested(
                examId,
                exam.getSnapshotVersion(),
                exam.getEndAt().plusHours(24)
        );
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("EXAM");
        event.setAggregateId(examId);
        event.setEventType(CACHE_EVENT);
        event.setPayload(toJson(eventPayload));
        event.setStatus(OutboxStatus.PENDING);
        outboxRepo.save(event);
        return exam;
    }

    private void validateQuota(Exam exam, List<QuestionSnapshotItem> questions) {
        Set<UUID> questionIds = new HashSet<>();
        long easy = 0;
        long medium = 0;
        long hard = 0;
        for (QuestionSnapshotItem item : questions) {
            if (!questionIds.add(item.questionId())) {
                throw new IllegalArgumentException("DUPLICATE_SNAPSHOT_QUESTION");
            }
            switch (QuestionDifficulty.valueOf(item.difficulty())) {
                case EASY -> easy++;
                case MEDIUM -> medium++;
                case HARD -> hard++;
            }
        }
        if (exam.getEasyCount() > easy
                || exam.getMediumCount() > medium
                || exam.getHardCount() > hard) {
            throw new IllegalArgumentException("INSUFFICIENT_COLLECTION_QUOTA");
        }
    }

    private PaperQuestionDTO toPaperQuestion(QuestionSnapshotItem item) {
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
        return new AnswerEntryDTO(
                item.questionId(),
                item.options().stream()
                        .filter(com.exam_service.exam_service.client.QuestionSnapshotOption::correct)
                        .map(com.exam_service.exam_service.client.QuestionSnapshotOption::optionId)
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
