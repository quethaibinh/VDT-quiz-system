package com.auth_service.auth_service.model.dto.students;

import java.util.List;

public record ResolveStudentsRequestDTO(List<String> studentIds) {
}
