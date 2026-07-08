package com.question_service.question_service.model.dto.questions;

public record QuestionMediaUrlResponseDTO(
        String imageObjectKey,
        String url,
        int expiresInSeconds
) {
}
