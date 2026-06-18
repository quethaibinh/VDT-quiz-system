package com.question_service.question_service.model.dto.questions;

import com.question_service.question_service.model.entity.ContentFormat;
import com.question_service.question_service.model.entity.OptionKey;

import java.util.UUID;

public record QuestionOptionResponseDTO(
        UUID id,
        OptionKey optionKey,
        String content,
        ContentFormat contentFormat,
        String explanation,
        Boolean correct
) {
}
