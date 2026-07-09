package com.auth_service.auth_service.model.dto.students;

import java.util.UUID;

public record StudentSummaryDTO(
        UUID id,
        String studentCode,
        String fullName,
        String displayName
) {
}
