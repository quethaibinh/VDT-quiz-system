package com.exam_service.exam_service.model.dto.cache;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RuntimeActivationDTO(
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
        String handleViolation
) {
    public RuntimeActivationDTO(
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
            String showResultPolicy
    ) {
        this(examId, snapshotVersion, code, title, subjectId, subjectName, ownerTeacherId,
                startAt, endAt, joinBeforeMinutes, joinAfterMinutes, showResultPolicy,
                5, "LOCK");
    }

    public RuntimeActivationDTO(
            UUID examId,
            int snapshotVersion,
            OffsetDateTime startAt,
            OffsetDateTime endAt,
            int joinBeforeMinutes,
            int joinAfterMinutes
    ) {
        this(examId, snapshotVersion, null, null, null, null, null,
                startAt, endAt, joinBeforeMinutes, joinAfterMinutes, null,
                5, "LOCK");
    }
}
