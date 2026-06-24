package com.examruntime_service.examruntime_service.model.dto.events;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Consumer-side copy cua contract producer Exam Service phat len Kafka.
 * Field name can giu trung de JSON tu producer round-trip duoc.
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
