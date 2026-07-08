package com.question_service.question_service.model.dto.questions;

import java.util.Map;

public record QuestionMediaSignResponseDTO(
        Map<String, String> urls,
        int expiresInSeconds
) {
}
