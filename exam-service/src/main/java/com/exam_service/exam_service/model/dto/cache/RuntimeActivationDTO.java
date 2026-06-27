package com.exam_service.exam_service.model.dto.cache;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RuntimeActivationDTO(
        UUID examId,
        int snapshotVersion,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int joinBeforeMinutes,
        int joinAfterMinutes
) {
}
