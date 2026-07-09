package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionServiceClient;
import com.exam_service.exam_service.model.dto.exams.ExamDetailDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
/**
 * Dieu phoi request chot lich ma khong giu transaction khi goi Question Service.
 */
public class TeacherExamSchedulingService {

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final QuestionServiceClient questionServiceClient;
    private final ExamSchedulingTransactionService transactionService;
    private final TeacherExamDraftService draftService;

    public TeacherExamSchedulingService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            QuestionServiceClient questionServiceClient,
            ExamSchedulingTransactionService transactionService,
            TeacherExamDraftService draftService
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.questionServiceClient = questionServiceClient;
        this.transactionService = transactionService;
        this.draftService = draftService;
    }

    public ExamDetailDTO schedule(UUID subjectId, UUID examId, UUID teacherId) {
        Exam exam = examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(
                examId, subjectId, teacherId
        ).orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND"));
        if (exam.getExamType() != ExamType.STANDARD_EXAM) {
            throw new ConflictException("LIVE_QUIZ_CANNOT_USE_EXAM_SCHEDULE");
        }
        // status o schedule roi thi bo qua
        if (exam.getStatus() == ExamStatus.SCHEDULED && questionRepo.existsByExamId(examId)) {
            return draftService.toDetail(exam);
        }
        if (exam.getStatus() != ExamStatus.DRAFT) { // neu khong phai schedule va draft thi khong duoc chinh sua
            throw new ConflictException("EXAM_NOT_EDITABLE");
        }
        // lay payload tu ben question service ve luu snapshot
        QuestionCollectionSnapshot snapshot = questionServiceClient.getExamSnapshot(
                subjectId,
                exam.getCollectionId(),
                teacherId
        );
        // tien hanh luu snapshot va doi trang thai sang SCHEDULE
        Exam scheduled = transactionService.schedule(
                subjectId,
                examId,
                teacherId,
                exam.getVersion(),
                snapshot
        );
        return draftService.toDetail(scheduled);
    }
}
