package com.exam_service.exam_service.model.dto.assignments;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record AssignmentRequestDTO(
        @NotEmpty(message = "studentIds is required")
        @Size(max = 100, message = "studentIds exceeds limit")
        List<UUID> studentIds
) {
}
