package com.result_service.result_service.model.dto.livequiz;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TeacherLiveQuizResultIndexDTO(
        UUID roomId,
        UUID examId,
        UUID subjectId,
        String subjectName,
        String roomCode,
        String quizTitle,
        OffsetDateTime closedAt,
        OffsetDateTime releasedAt,
        int participantCount,
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore
) {
}
