package com.result_service.result_service.client;

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
        String showResultPolicy
) {
}
