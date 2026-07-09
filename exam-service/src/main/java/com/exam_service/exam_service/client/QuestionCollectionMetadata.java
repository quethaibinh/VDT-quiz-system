package com.exam_service.exam_service.client;

import java.util.UUID;

public record QuestionCollectionMetadata(
        UUID collectionId,
        UUID subjectId,
        String subjectName,
        String name,
        long easy,
        long medium,
        long hard
) {
}
