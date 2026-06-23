package com.examruntime_service.examruntime_service.config.security;

public record ExamRuntimeUserPrincipal(
        String userId,
        String username,
        String role
) {
}
