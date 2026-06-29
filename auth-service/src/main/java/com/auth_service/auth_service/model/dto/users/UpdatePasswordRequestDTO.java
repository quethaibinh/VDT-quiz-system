package com.auth_service.auth_service.model.dto.users;

public record UpdatePasswordRequestDTO(
        String currentPassword,
        String newPassword,
        String confirmPassword
) {
}
