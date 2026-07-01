package com.exam_service.exam_service.model.dto.livequiz;

import java.util.UUID;

public record LiveQuizPrepareResponseDTO(
        LiveQuizDetailDTO quiz,
        UUID roomId,
        String roomCode,
        String roomStatus
) {
}
