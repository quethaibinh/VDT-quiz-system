package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ExamClosingTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(ExamClosingTransactionService.class);

    private final ExamRepo examRepo;
    private final Clock clock;

    public ExamClosingTransactionService(ExamRepo examRepo, Clock clock) {
        this.examRepo = examRepo;
        this.clock = clock;
    }

    /**
     * Moi ca thi dong trong transaction rieng de mot loi khong lam dung ca batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean closeIfEnded(UUID examId, long gracePeriodSeconds) {
        Exam exam = examRepo.findByIdForUpdate(examId).orElse(null);
        if (exam == null || exam.getStatus() != ExamStatus.ACTIVE || exam.getEndAt() == null) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime closeDueAt = exam.getEndAt().plusSeconds(Math.max(0, gracePeriodSeconds));
        if (now.isBefore(closeDueAt)) {
            return false;
        }

        exam.setStatus(ExamStatus.CLOSED);
        exam.setClosedAt(now);
        examRepo.save(exam);
        logger.info("Closed exam {}. closeDueAt={}", examId, closeDueAt);
        return true;
    }
}
