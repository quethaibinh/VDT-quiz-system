package com.result_service.result_service.model.dto.results;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentResultDetailDTO(
        UUID examId,
        String code,
        String title,
        UUID subjectId,
        String subjectName,
        ResultVisibilityStateDTO visibilityState,
        String message,
        OffsetDateTime availableAt,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal percentage,
        Integer rank,
        Integer gradedCount,
        int totalQuestions,
        int correctCount,
        int wrongCount,
        int blankCount,
        OffsetDateTime submittedAt,
        OffsetDateTime gradedAt,
        OffsetDateTime releasedAt
) {
}
