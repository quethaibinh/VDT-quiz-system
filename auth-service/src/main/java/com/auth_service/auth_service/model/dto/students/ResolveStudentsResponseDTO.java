package com.auth_service.auth_service.model.dto.students;

import java.util.List;
import java.util.UUID;

public record ResolveStudentsResponseDTO(
        List<StudentSummaryDTO> students,
        List<UUID> missingStudentIds,
        List<UUID> inactiveStudentIds,
        List<UUID> nonStudentIds
) {
}
