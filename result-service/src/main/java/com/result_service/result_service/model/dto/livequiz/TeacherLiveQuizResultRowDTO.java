package com.result_service.result_service.model.dto.livequiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TeacherLiveQuizResultRowDTO(
        UUID resultId,
        UUID roomId,
        UUID examId,
        UUID participantId,
        UUID studentId,
        String studentCode,
        String studentName,
        int finalRank,
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
        OffsetDateTime releasedAt
) {
}
