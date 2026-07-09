package com.question_service.question_service.model.dto.collections;

public record CollectionStatsDTO(
        long questionCount,
        long easy,
        long medium,
        long hard
) {
    public static CollectionStatsDTO empty() {
        return new CollectionStatsDTO(0, 0, 0, 0);
    }
}
