package com.question_service.question_service.client;

import java.util.List;
import java.util.UUID;

public record ResolveTeachersResponse(
        List<TeacherSummary> teachers,
        List<UUID> missingTeacherIds,
        List<UUID> nonTeacherIds
) {
}
