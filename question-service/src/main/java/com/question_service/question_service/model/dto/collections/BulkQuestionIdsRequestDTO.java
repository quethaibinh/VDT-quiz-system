package com.question_service.question_service.model.dto.collections;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;
import java.util.UUID;

public record BulkQuestionIdsRequestDTO(
        @NotEmpty(message = "questionIds is required") List<UUID> questionIds
) {
}
