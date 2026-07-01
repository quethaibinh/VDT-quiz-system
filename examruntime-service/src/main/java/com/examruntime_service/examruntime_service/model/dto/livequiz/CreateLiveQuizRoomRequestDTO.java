package com.examruntime_service.examruntime_service.model.dto.livequiz;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateLiveQuizRoomRequestDTO(
        @NotNull(message = "examId is required")
        UUID examId,
        @NotNull(message = "ownerTeacherId is required")
        UUID ownerTeacherId,
        @Min(value = 1, message = "snapshotVersion must be positive")
        int snapshotVersion,
        @NotBlank(message = "joinPolicy is required")
        String joinPolicy
) {
}
