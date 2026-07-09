package com.question_service.question_service.client;

import java.util.UUID;

public record TeacherSummary(
        UUID id,
        String teacherCode,
        String fullName,
        String displayName,
        String email,
        String status
) {
}
