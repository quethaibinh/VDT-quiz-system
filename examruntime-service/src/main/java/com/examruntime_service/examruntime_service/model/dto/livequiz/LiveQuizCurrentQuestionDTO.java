package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record LiveQuizCurrentQuestionDTO(
        UUID participantId,
        UUID questionId,
        int questionPosition,
        int totalQuestions,
        int answeredCount,
        BigDecimal totalScore,
        BigDecimal maxScore,
        Integer currentRank,
        String type,
        String content,
        String contentFormat,
        List<PaperOptionDTO> options,
        OffsetDateTime startedAt,
        OffsetDateTime endsAt,
        OffsetDateTime serverTime
) {
}
