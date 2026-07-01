package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentLiveQuizStateDTO(
        UUID roomId,
        UUID examId,
        UUID participantId,
        LiveQuizRoomStatus roomStatus,
        LiveQuizParticipantStatus participantStatus,
        int answeredCount,
        int totalQuestions,
        BigDecimal totalScore,
        BigDecimal maxScore,
        OffsetDateTime serverTime,
        OffsetDateTime currentQuestionEndsAt
) {
}
