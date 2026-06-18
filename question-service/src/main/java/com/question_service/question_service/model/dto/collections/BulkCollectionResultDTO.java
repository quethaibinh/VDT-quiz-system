package com.question_service.question_service.model.dto.collections;

public record BulkCollectionResultDTO(
        int requestedCount,
        int matchedCount,
        int addedCount,
        int removedCount,
        int skippedCount,
        long totalQuestionCount
) {
}
