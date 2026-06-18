package com.question_service.question_service.model.dto.collections;

import java.util.List;
import java.util.UUID;

public record AddByFilterRequestDTO(
        String keyword,
        UUID topicId,
        List<String> difficulties,
        List<String> visibilities,
        String ownerScope,
        String questionType,
        List<UUID> excludeQuestionIds
) {
}
