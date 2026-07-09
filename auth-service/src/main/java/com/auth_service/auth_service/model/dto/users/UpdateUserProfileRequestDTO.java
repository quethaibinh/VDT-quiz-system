package com.auth_service.auth_service.model.dto.users;

import com.auth_service.auth_service.model.entity.UserGender;

import java.time.LocalDate;

public record UpdateUserProfileRequestDTO(
        String fullName,
        String displayName,
        String email,
        LocalDate birthDate,
        UserGender gender
) {
}
