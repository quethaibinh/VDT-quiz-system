package com.question_service.question_service.model.dto.collections;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CollectionRequestDTO(
        @NotBlank(message = "name is required") String name,
        String description,
        @NotNull(message = "visibility is required") String visibility
) {
}
