package com.exam_service.exam_service.client;

import java.util.UUID;

public record ResolvedStudent(
        UUID id,
        String studentCode,
        String fullName,
        String displayName
) {
    public String snapshotName() {
        return displayName == null || displayName.isBlank() ? fullName : displayName;
    }
}
