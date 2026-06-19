package com.question_service.question_service.model.dto.collections;

import java.util.UUID;

public record ExamCollectionMetadataDTO(
        UUID collectionId,
        UUID subjectId,
        String subjectName,
        String name,
        long easy,
        long medium,
        long hard
) {
}
