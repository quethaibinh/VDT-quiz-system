package com.auth_service.auth_service.model.dto.teachers;

import java.util.List;
import java.util.UUID;

public record ResolveTeachersResponseDTO(
        List<TeacherSummaryDTO> teachers,
        List<UUID> missingTeacherIds,
        List<UUID> nonTeacherIds
) {
}
