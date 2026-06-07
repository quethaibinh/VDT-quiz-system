package com.auth_service.auth_service.config.security;

public record GatewayUserPrincipal(
        String userId,
        String username,
        String role
) {
}
