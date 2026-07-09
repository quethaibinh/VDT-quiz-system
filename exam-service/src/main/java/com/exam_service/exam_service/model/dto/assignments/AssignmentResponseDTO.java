package com.exam_service.exam_service.model.dto.assignments;

import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;

import java.time.LocalDateTime;
import java.util.UUID;

public record AssignmentResponseDTO(
        UUID id,
        UUID studentId,
        String studentCode,
        String studentName,
        AssignmentStatus status,
        LocalDateTime assignedAt,
        LocalDateTime removedAt
) {
}
