package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record StudentLiveQuizJoinResponseDTO(
        UUID roomId,
        UUID examId,
        UUID participantId,
        String roomCode,
        String quizTitle,
        String subjectName,
        LiveQuizParticipantStatus status,
        LiveQuizRoomStatus roomStatus,
        int totalQuestions,
        int participantCount,
        Integer currentRank,
        OffsetDateTime serverTime
) {
}
