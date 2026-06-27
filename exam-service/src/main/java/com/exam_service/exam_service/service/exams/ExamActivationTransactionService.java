package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.dto.outbox.ExamActivatedEvent;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.repository.OutboxEventRepo;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class ExamActivationTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(ExamActivationTransactionService.class);

    private final ExamRepo examRepo;
    private final OutboxEventRepo outboxRepo;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ExamActivationTransactionService(ExamRepo examRepo, OutboxEventRepo outboxRepo, ObjectMapper objectMapper, Clock clock) {
        this.examRepo = examRepo;
        this.outboxRepo = outboxRepo;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Kiem tra lai nguong kich hoat trong pessimistic lock.
     * Moi ca thi dung giao dich rieng de loi mot ca khong lam hong ca batch.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean activateIfDue(UUID examId) {
        Exam exam = examRepo.findByIdForUpdate(examId).orElse(null);
        if (exam == null) {
            return false;
        }

        if (exam.getStatus() != ExamStatus.SCHEDULED) {
            return false;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        OffsetDateTime activationDueAt = exam.getStartAt()
                .minusMinutes(exam.getJoinBeforeMinutes())
                .minusMinutes(30);

        if (now.isBefore(activationDueAt)) {
            return false;
        }

        logger.info("Activating exam {}. Threshold was: {}", examId, activationDueAt);

        exam.setStatus(ExamStatus.ACTIVE);
        exam.setActivatedAt(now);
        examRepo.save(exam);

        UUID outboxRowId = UUID.randomUUID();
        ExamActivatedEvent payload = new ExamActivatedEvent(
                outboxRowId,
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                exam.getId(),
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
                exam.getShowResultPolicy() != null ? exam.getShowResultPolicy().name() : null,
                now
        );

        String payloadJson;
        try {
            payloadJson = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize ExamActivatedEvent", e);
        }

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setAggregateType("EXAM");
        outboxEvent.setAggregateId(exam.getId());
        outboxEvent.setEventType(ExamActivatedEvent.EVENT_TYPE);
        outboxEvent.setPayload(payloadJson);
        outboxEvent.setStatus(OutboxStatus.PENDING);
        outboxRepo.save(outboxEvent);

        return true;
    }
}
