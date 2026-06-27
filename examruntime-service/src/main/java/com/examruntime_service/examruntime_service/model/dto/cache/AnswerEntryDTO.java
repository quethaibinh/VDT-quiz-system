package com.examruntime_service.examruntime_service.model.dto.cache;

import java.util.List;
import java.util.UUID;

public record AnswerEntryDTO(
        UUID questionId,
        List<UUID> correctOptionIds,
        double score
) {
}
