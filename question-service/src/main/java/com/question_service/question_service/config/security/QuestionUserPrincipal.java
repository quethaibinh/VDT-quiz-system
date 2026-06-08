package com.question_service.question_service.config.security;

public record QuestionUserPrincipal(
        String userId,
        String username,
        String role
) {
}
