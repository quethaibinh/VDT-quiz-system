package com.examruntime_service.examruntime_service.model.dto.events;

import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record SubmissionCreatedEvent(
        UUID eventId,
        String eventType,
        String producer,
        OffsetDateTime occurredAt,
        UUID submissionId,
        UUID sessionId,
        UUID examId,
        UUID studentId,
        int attemptNo,
        int snapshotVersion,
        SubmitReason submitReason,
        OffsetDateTime submittedAt,
        List<StudentAnswerDTO> answerSnapshot,
        Map<String, Object> paperSnapshot
) {
    public static final String EVENT_TYPE = "SubmissionCreated";
    public static final String PRODUCER = "examruntime-service";
}
