package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;

import java.util.UUID;

public record LiveQuizRoomLookupDTO(
        UUID examId,
        UUID roomId,
        String roomCode,
        LiveQuizRoomStatus status
) {
}
