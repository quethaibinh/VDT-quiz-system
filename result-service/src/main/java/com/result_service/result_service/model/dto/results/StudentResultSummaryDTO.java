package com.result_service.result_service.model.dto.results;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentResultSummaryDTO(
        UUID examId,
        String code,
        String title,
        String subjectName,
        ResultVisibilityStateDTO visibilityState,
        String message,
        OffsetDateTime availableAt,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal percentage,
        Integer rank,
        Integer gradedCount,
        OffsetDateTime submittedAt,
        OffsetDateTime gradedAt,
        OffsetDateTime releasedAt
) {
}
