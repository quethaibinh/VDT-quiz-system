package com.exam_service.exam_service.model.dto.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event published to Kafka when an exam is activated and runtime preparation starts.
 */
public record ExamActivatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID examId,
        int snapshotVersion,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int joinBeforeMinutes,
        int joinAfterMinutes,
        OffsetDateTime occurredAt
) {
    public static final String EVENT_TYPE = "ExamActivated";
    public static final int EVENT_VERSION = 1;
}
