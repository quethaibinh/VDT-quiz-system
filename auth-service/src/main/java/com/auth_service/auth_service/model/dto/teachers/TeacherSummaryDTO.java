package com.auth_service.auth_service.model.dto.teachers;

import java.util.UUID;

public record TeacherSummaryDTO(
        UUID id,
        String teacherCode,
        String fullName,
        String displayName,
        String email,
        String status
) {
}
