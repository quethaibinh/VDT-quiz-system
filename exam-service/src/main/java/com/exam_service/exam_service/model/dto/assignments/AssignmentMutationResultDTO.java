package com.exam_service.exam_service.model.dto.assignments;

import java.util.List;

public record AssignmentMutationResultDTO(
        int requestedCount,
        int assignedCount,
        int unchangedCount,
        List<AssignmentResponseDTO> assignments
) {
}
