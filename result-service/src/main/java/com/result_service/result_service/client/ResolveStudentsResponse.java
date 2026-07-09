package com.result_service.result_service.client;

import java.util.List;
import java.util.UUID;

public record ResolveStudentsResponse(
        List<StudentSummary> students,
        List<UUID> missingStudentIds,
        List<UUID> inactiveStudentIds,
        List<UUID> nonStudentIds
) {
}
