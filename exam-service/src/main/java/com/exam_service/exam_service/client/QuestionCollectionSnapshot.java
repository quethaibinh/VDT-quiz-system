package com.exam_service.exam_service.client;

import java.util.List;
import java.util.UUID;

public record QuestionCollectionSnapshot(
        UUID collectionId,
        UUID subjectId,
        String subjectName,
        String name,
        List<QuestionSnapshotItem> questions
) {
}
