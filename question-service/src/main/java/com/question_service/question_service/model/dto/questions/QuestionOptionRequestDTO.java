package com.question_service.question_service.model.dto.questions;

import jakarta.validation.constraints.NotBlank;

public record QuestionOptionRequestDTO(
        @NotBlank(message = "optionKey is required") String optionKey,
        @NotBlank(message = "option content is required") String content,
        String contentFormat,
        String explanation,
        boolean correct
) {
}
