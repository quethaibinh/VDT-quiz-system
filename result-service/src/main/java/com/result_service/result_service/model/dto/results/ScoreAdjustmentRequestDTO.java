package com.result_service.result_service.model.dto.results;

import java.math.BigDecimal;

public record ScoreAdjustmentRequestDTO(
        BigDecimal adjustedScore,
        String reason
) {
}
