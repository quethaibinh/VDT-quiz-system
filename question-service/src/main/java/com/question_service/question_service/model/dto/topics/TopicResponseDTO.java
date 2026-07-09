package com.question_service.question_service.model.dto.topics;

import java.util.UUID;

public record TopicResponseDTO(
        UUID id,
        UUID subjectId,
        String name,
        String description,
        String slug,
        UUID parentTopicId
) {
}
