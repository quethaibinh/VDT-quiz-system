package com.result_service.result_service.model.dto.results;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TeacherResultRowDTO(
        UUID resultId,
        UUID examId,
        UUID studentId,
        String studentCode,
        String studentName,
        BigDecimal originalScore,
        BigDecimal adjustedScore,
        BigDecimal effectiveScore,
        BigDecimal maxScore,
        BigDecimal percentage,
        int rank,
        int totalQuestions,
        int correctCount,
        int wrongCount,
        int blankCount,
        ResultReviewStatusDTO reviewStatus,
        ResultVisibilityStateDTO visibilityState,
        OffsetDateTime submittedAt,
        OffsetDateTime gradedAt,
        OffsetDateTime releasedAt
) {
}
