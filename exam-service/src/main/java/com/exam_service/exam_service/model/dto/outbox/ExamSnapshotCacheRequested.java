package com.exam_service.exam_service.model.dto.outbox;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamSnapshotCacheRequested(
        UUID examId,
        int snapshotVersion,
        OffsetDateTime expiresAt
) {
}
