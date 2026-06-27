package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import com.result_service.result_service.model.entity.GradingJob;
import com.result_service.result_service.model.entity.InboxMessage;
import com.result_service.result_service.model.entity.enums.GradingJobStatus;
import com.result_service.result_service.model.entity.enums.InboxMessageStatus;
import com.result_service.result_service.repository.GradingJobRepo;
import com.result_service.result_service.repository.InboxMessageRepo;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;

@Service
public class GradingFailureRecorder {

    private final InboxMessageRepo inboxMessageRepo;
    private final GradingJobRepo gradingJobRepo;
    private final Clock clock;

    public GradingFailureRecorder(
            InboxMessageRepo inboxMessageRepo,
            GradingJobRepo gradingJobRepo,
            Clock clock
    ) {
        this.inboxMessageRepo = inboxMessageRepo;
        this.gradingJobRepo = gradingJobRepo;
        this.clock = clock;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            SubmissionCreatedEvent event,
            String rawJson,
            String payloadHash,
            RuntimeException exception
    ) {
        if (event == null || event.eventId() == null) {
            return;
        }
        try {
            if (!recordInboxFailure(event, payloadHash)) {
                return;
            }
            recordJobFailure(event, rawJson, exception);
        } catch (DataIntegrityViolationException ignored) {
            // Consumer khac co the da xu ly xong cung message trong luc nhanh failure dang chay.
        }
    }

    private boolean recordInboxFailure(SubmissionCreatedEvent event, String payloadHash) {
        InboxMessage inbox = inboxMessageRepo.findById(event.eventId()).orElseGet(InboxMessage::new);
        if (inbox.getStatus() == InboxMessageStatus.PROCESSED) {
            return false;
        }
        inbox.setMessageId(event.eventId());
        inbox.setEventType(event.eventType() != null ? event.eventType() : "UNKNOWN");
        inbox.setProducer(event.producer() != null ? event.producer() : "UNKNOWN");
        inbox.setReceivedAt(inbox.getReceivedAt() != null ? inbox.getReceivedAt() : OffsetDateTime.now(clock));
        inbox.setStatus(InboxMessageStatus.FAILED);
        inbox.setPayloadHash(payloadHash);
        inboxMessageRepo.save(inbox);
        return true;
    }

    private void recordJobFailure(
            SubmissionCreatedEvent event,
            String rawJson,
            RuntimeException exception
    ) {
        if (event.submissionId() == null || event.examId() == null || event.studentId() == null) {
            return;
        }

        GradingJob job = gradingJobRepo.findByMessageId(event.eventId()).orElseGet(GradingJob::new);
        job.setMessageId(event.eventId());
        job.setSubmissionId(event.submissionId());
        job.setExamId(event.examId());
        job.setStudentId(event.studentId());
        job.setStatus(GradingJobStatus.FAILED);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setFinishedAt(OffsetDateTime.now(clock));
        job.setErrorCode(exception.getClass().getSimpleName());
        job.setErrorMessage(exception.getMessage());
        job.setPayload(rawJson);
        gradingJobRepo.save(job);
    }
}
