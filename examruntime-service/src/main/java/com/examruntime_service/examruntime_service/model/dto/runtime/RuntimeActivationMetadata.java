package com.examruntime_service.examruntime_service.model.dto.runtime;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Value luu tai runtime:exam:{examId}:activation.
 * Join/resume API sau nay dung metadata nay de tinh cua so vao phong/vao thi.
 */
public record RuntimeActivationMetadata(
        UUID examId,
        int snapshotVersion,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int joinBeforeMinutes,
        int joinAfterMinutes,
        String status,
        OffsetDateTime readyAt,
        String snapshotSource
) {
    public static final String STATUS_READY = "READY";
    public static final String SOURCE_REDIS = "REDIS";
    public static final String SOURCE_EXAM_SERVICE_FALLBACK = "EXAM_SERVICE_FALLBACK";
}
