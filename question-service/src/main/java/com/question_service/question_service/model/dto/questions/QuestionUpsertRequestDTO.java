package com.question_service.question_service.model.dto.questions;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record QuestionUpsertRequestDTO(
        @NotNull(message = "topicId is required") UUID topicId,
        @NotBlank(message = "questionType is required") String questionType,
        @NotBlank(message = "content is required") String content,
        String contentFormat,
        String explanation,
        @NotBlank(message = "difficulty is required") String difficulty,
        Double defaultScore,
        Integer estimatedSecond,
        String visibility,
        @NotNull(message = "options are required")
        @Size(min = 2, message = "at least two options are required")
        List<@Valid QuestionOptionRequestDTO> options
) {
}
