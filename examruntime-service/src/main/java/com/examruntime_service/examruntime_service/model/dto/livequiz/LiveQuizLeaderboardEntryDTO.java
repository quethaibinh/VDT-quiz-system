package com.examruntime_service.examruntime_service.model.dto.livequiz;

import java.math.BigDecimal;
import java.util.UUID;

public record LiveQuizLeaderboardEntryDTO(
        int rank,
        UUID participantId,
        UUID studentId,
        String studentName,
        BigDecimal totalScore,
        int answeredCount,
        int correctCount,
        int timeoutCount,
        Integer averageResponseMs,
        boolean finished
) {
}
