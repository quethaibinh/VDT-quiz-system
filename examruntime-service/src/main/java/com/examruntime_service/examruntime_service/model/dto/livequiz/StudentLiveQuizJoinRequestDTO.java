package com.examruntime_service.examruntime_service.model.dto.livequiz;

import jakarta.validation.constraints.NotBlank;

public record StudentLiveQuizJoinRequestDTO(
        @NotBlank(message = "ROOM_CODE_REQUIRED")
        String code
) {
}
