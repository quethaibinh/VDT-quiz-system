package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.dto.cache.AnswerEntryDTO;
import com.exam_service.exam_service.model.dto.cache.ExamAnswerKeyDTO;
import com.exam_service.exam_service.model.dto.cache.ExamPaperPoolDTO;
import com.exam_service.exam_service.model.dto.cache.PaperQuestionDTO;
import com.exam_service.exam_service.model.dto.cache.RuntimeActivationDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

@Service
/**
 * Doc snapshot PostgreSQL theo hai projection tach biet de khong lo dap an.
 */
public class ExamSnapshotReadService {

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final ObjectMapper objectMapper;

    public ExamSnapshotReadService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            ObjectMapper objectMapper
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public ExamPaperPoolDTO getPaperPool(UUID examId) {
        Exam exam = requireSnapshotExam(examId);
        List<PaperQuestionDTO> questions = rows(examId).stream()
                .map(row -> read(row.getQuestionSnapshot(), PaperQuestionDTO.class))
                .toList();
        return new ExamPaperPoolDTO(
                examId,
                exam.getSnapshotVersion(),
                exam.getEasyCount(),
                exam.getMediumCount(),
                exam.getHardCount(),
                questions
        );
    }

    @Transactional(readOnly = true)
    public ExamAnswerKeyDTO getAnswerKey(UUID examId) {
        Exam exam = requireSnapshotExam(examId);
        List<AnswerEntryDTO> answers = rows(examId).stream()
                .map(row -> read(row.getAnswerKeySnapshot(), AnswerEntryDTO.class))
                .toList();
        return new ExamAnswerKeyDTO(examId, exam.getSnapshotVersion(), answers);
    }

    @Transactional(readOnly = true)
    public RuntimeActivationDTO getRuntimeActivation(UUID examId) {
        Exam exam = requireSnapshotExam(examId);
        return new RuntimeActivationDTO(
                examId,
                exam.getSnapshotVersion(),
                exam.getCode(),
                exam.getTitle(),
                exam.getSubjectId(),
                exam.getSubjectNameSnapshot(),
                exam.getCreatedByTeacherId(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getJoinBeforeMinutes(),
                exam.getJoinAfterMinutes(),
                exam.getShowResultPolicy() != null ? exam.getShowResultPolicy().name() : null
        );
    }

    private Exam requireSnapshotExam(UUID examId) {
        Exam exam = examRepo.findById(examId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND"));
        if (exam.getStatus() == ExamStatus.DRAFT
                || exam.getStatus() == ExamStatus.CANCELLED
                || exam.getSnapshotVersion() < 1) {
            throw new ConflictException("EXAM_SNAPSHOT_NOT_READY");
        }
        return exam;
    }

    private List<ExamQuestion> rows(UUID examId) {
        List<ExamQuestion> rows = questionRepo.findAllByExamIdOrderBySortOrderAsc(examId);
        if (rows.isEmpty()) {
            throw new ConflictException("EXAM_SNAPSHOT_NOT_READY");
        }
        return rows;
    }

    private <T> T read(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalStateException("INVALID_STORED_EXAM_SNAPSHOT", exception);
        }
    }
}
