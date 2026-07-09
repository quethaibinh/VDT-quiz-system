package com.auth_service.auth_service.model.dto.users;

import com.auth_service.auth_service.model.entity.UserType;

import java.util.UUID;

public record AdminUserSummaryDTO(
        UUID id,
        String username,
        UserType userType,
        String studentCode,
        String teacherCode,
        String fullName,
        String displayName,
        String email,
        String status
) {
}
