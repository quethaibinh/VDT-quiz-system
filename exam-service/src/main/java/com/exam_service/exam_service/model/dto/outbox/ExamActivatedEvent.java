package com.exam_service.exam_service.model.dto.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Event gui sang Kafka khi ca thi duoc kich hoat de runtime bat dau chuan bi.
 */
public record ExamActivatedEvent(
        UUID eventId,
        String eventType,
        int eventVersion,
        UUID examId,
        int snapshotVersion,
        String code,
        String title,
        UUID subjectId,
        String subjectName,
        UUID ownerTeacherId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int joinBeforeMinutes,
        int joinAfterMinutes,
        String showResultPolicy,
        int maxViolationAllowed,
        String handleViolation,
        OffsetDateTime occurredAt
) {
    public static final String EVENT_TYPE = "ExamActivated";
    public static final int EVENT_VERSION = 1;

    public ExamActivatedEvent(
            UUID eventId,
            String eventType,
            int eventVersion,
            UUID examId,
            int snapshotVersion,
            String code,
            String title,
            UUID subjectId,
            String subjectName,
            UUID ownerTeacherId,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int joinBeforeMinutes,
            int joinAfterMinutes,
            String showResultPolicy,
            OffsetDateTime occurredAt
    ) {
        this(eventId, eventType, eventVersion, examId, snapshotVersion, code, title,
                subjectId, subjectName, ownerTeacherId, startAt, endAt, joinBeforeMinutes,
                joinAfterMinutes, showResultPolicy, 5, "LOCK", occurredAt);
    }

    public ExamActivatedEvent(
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
        this(eventId, eventType, eventVersion, examId, snapshotVersion, null, null,
                null, null, null, startAt, endAt, joinBeforeMinutes, joinAfterMinutes,
                null, 5, "LOCK", occurredAt);
    }
}
