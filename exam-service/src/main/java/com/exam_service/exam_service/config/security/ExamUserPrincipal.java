package com.exam_service.exam_service.config.security;

public record ExamUserPrincipal(
        String userId,
        String username,
        String role
) {
}
