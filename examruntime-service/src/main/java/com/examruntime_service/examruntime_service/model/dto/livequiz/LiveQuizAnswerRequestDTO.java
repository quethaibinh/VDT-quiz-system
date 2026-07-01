package com.examruntime_service.examruntime_service.model.dto.livequiz;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record LiveQuizAnswerRequestDTO(
        @NotNull(message = "QUESTION_ID_REQUIRED")
        UUID questionId,
        List<UUID> selectedOptionIds
) {
}
