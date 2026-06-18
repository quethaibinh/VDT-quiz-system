package com.question_service.question_service.model.dto.collections;

import com.question_service.question_service.model.entity.CollectionStatus;
import com.question_service.question_service.model.entity.CollectionVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record CollectionResponseDTO(
        UUID id,
        UUID subjectId,
        UUID ownerTeacherId,
        String name,
        String description,
        CollectionVisibility visibility,
        CollectionStatus status,
        boolean editable,
        CollectionStatsDTO stats,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
