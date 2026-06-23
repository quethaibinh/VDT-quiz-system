package com.auth_service.auth_service.model.dto.users;

import com.auth_service.auth_service.model.entity.UserGender;
import com.auth_service.auth_service.model.entity.UserType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserDetailDTO(
        UUID id,
        String username,
        UserType userType,
        String studentCode,
        String teacherCode,
        String fullName,
        String displayName,
        String email,
        LocalDate birthDate,
        UserGender gender,
        String avatarUrl,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
