package com.result_service.result_service.model.dto.livequiz;

import com.result_service.result_service.model.dto.common.PageResponseDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TeacherLiveQuizResultsDTO(
        UUID roomId,
        UUID examId,
        String roomCode,
        String quizTitle,
        UUID subjectId,
        String subjectName,
        OffsetDateTime closedAt,
        OffsetDateTime releasedAt,
        int participantCount,
        BigDecimal averageScore,
        BigDecimal highestScore,
        BigDecimal lowestScore,
        PageResponseDTO<TeacherLiveQuizResultRowDTO> rows
) {
}
