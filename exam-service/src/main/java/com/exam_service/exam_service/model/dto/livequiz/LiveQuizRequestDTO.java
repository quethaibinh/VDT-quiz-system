package com.exam_service.exam_service.model.dto.livequiz;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record LiveQuizRequestDTO(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title is too long")
        String title,
        @Size(max = 4000, message = "description is too long")
        String description,
        @NotNull(message = "collectionId is required")
        UUID collectionId,
        Boolean shuffleQuestions,
        Boolean showLeaderboard,
        Boolean showCorrectAnswer,
        String joinPolicy
) {
}
