package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class SubmissionEventParser {

    private final ObjectMapper objectMapper;

    public SubmissionEventParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SubmissionCreatedEvent parse(String rawJson) {
        try {
            SubmissionCreatedEvent event = objectMapper.readValue(rawJson, SubmissionCreatedEvent.class);
            validateEnvelope(event);
            return event;
        } catch (RuntimeException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("INVALID_SUBMISSION_CREATED_EVENT_JSON", exception);
        }
    }

    private void validateEnvelope(SubmissionCreatedEvent event) {
        // Envelope chi can cac truong dinh danh va metadata bat buoc cua event.
        if (event == null
                || event.eventId() == null
                || event.submissionId() == null
                || event.sessionId() == null
                || event.examId() == null
                || event.studentId() == null) {
            throw new IllegalArgumentException("SUBMISSION_CREATED_EVENT_IDS_REQUIRED");
        }
        if (!SubmissionCreatedEvent.EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("UNSUPPORTED_SUBMISSION_EVENT_TYPE");
        }
        if (event.submittedAt() == null) {
            throw new IllegalArgumentException("SUBMITTED_AT_REQUIRED");
        }
        if (event.attemptNo() <= 0 || event.snapshotVersion() <= 0) {
            throw new IllegalArgumentException("INVALID_SUBMISSION_ATTEMPT_OR_SNAPSHOT");
        }
    }
}
