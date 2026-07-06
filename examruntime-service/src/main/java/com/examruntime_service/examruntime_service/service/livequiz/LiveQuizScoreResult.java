package com.examruntime_service.examruntime_service.service.livequiz;

import java.math.BigDecimal;

public record LiveQuizScoreResult(
        BigDecimal scoreAwarded,
        int responseTimeMs,
        BigDecimal scoreRatio
) {
}
