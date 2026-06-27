package com.result_service.result_service.config.security;

public record ResultUserPrincipal(
        String userId,
        String username,
        String role
) {
}
