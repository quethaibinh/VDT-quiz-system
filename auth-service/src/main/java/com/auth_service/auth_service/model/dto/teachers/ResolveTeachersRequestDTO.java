package com.auth_service.auth_service.model.dto.teachers;

import java.util.List;

public record ResolveTeachersRequestDTO(List<String> teacherIds) {
}
