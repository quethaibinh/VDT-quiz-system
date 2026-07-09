package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LiveQuizAnswerResponseDTO(
        UUID questionId,
        int questionPosition,
        LiveQuizAnswerStatus answerStatus,
        boolean correct,
        List<LiveQuizSelectedOptionResultDTO> selectedOptionResults,
        BigDecimal scoreAwarded,
        BigDecimal maxScore,
        Integer responseTimeMs,
        BigDecimal scoreRatio,
        BigDecimal totalScore,
        boolean nextQuestionAvailable,
        boolean finished
) {
}
