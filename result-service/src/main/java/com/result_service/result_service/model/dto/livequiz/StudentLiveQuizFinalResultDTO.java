package com.result_service.result_service.model.dto.livequiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentLiveQuizFinalResultDTO(
        UUID roomId,
        UUID examId,
        String roomCode,
        String quizTitle,
        UUID subjectId,
        String subjectName,
        int finalRank,
        int participantCount,
        BigDecimal score,
        BigDecimal maxScore,
        BigDecimal percentage,
        int answeredCount,
        int totalQuestions,
        int correctCount,
        int wrongCount,
        int timeoutCount,
        int notReachedCount,
        Integer averageResponseMs,
        OffsetDateTime finishedAt,
        OffsetDateTime closedAt,
        OffsetDateTime releasedAt
) {
}
