package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveQuizRoomDTO(
        UUID roomId,
        UUID examId,
        String roomCode,
        String quizTitle,
        UUID subjectId,
        String subjectName,
        int questionCount,
        boolean showLeaderboard,
        UUID ownerTeacherId,
        LiveQuizRoomStatus status,
        OffsetDateTime openedAt,
        OffsetDateTime startedAt,
        OffsetDateTime closedAt
) {
}
