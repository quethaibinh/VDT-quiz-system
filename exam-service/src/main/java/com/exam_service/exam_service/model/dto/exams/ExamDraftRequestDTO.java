package com.exam_service.exam_service.model.dto.exams;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamDraftRequestDTO(
        @NotBlank(message = "title is required")
        @Size(max = 255, message = "title is too long")
        String title,
        @Size(max = 4000, message = "description is too long")
        String description,
        @NotNull(message = "collectionId is required")
        UUID collectionId,
        @Min(value = 0, message = "easyCount must be non-negative")
        int easyCount,
        @Min(value = 0, message = "mediumCount must be non-negative")
        int mediumCount,
        @Min(value = 0, message = "hardCount must be non-negative")
        int hardCount,
        @NotNull(message = "startAt is required")
        OffsetDateTime startAt,
        @Min(value = 1, message = "durationMinutes must be positive")
        int durationMinutes,
        @Min(value = 0, message = "joinBeforeMinutes must be non-negative")
        int joinBeforeMinutes,
        @Min(value = 0, message = "joinAfterMinutes must be non-negative")
        int joinAfterMinutes,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        @NotBlank(message = "showResultPolicy is required")
        String showResultPolicy,
        boolean autoSubmit,
        boolean requireFullscreen,
        @Min(value = 0, message = "maxViolationAllowed must be non-negative")
        int maxViolationAllowed,
        @NotBlank(message = "handleViolation is required")
        String handleViolation
) {
}
