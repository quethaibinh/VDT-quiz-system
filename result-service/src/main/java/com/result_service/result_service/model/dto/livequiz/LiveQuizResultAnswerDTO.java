package com.result_service.result_service.model.dto.livequiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record LiveQuizResultAnswerDTO(
        UUID questionId,
        int questionPosition,
        List<UUID> selectedOptionIds,
        List<UUID> correctOptionIds,
        boolean correct,
        BigDecimal scoreAwarded,
        BigDecimal maxScore,
        String answerStatus,
        Integer responseTimeMs,
        OffsetDateTime answeredAt,
        Map<String, Object> questionSnapshot
) {
}
