package com.result_service.result_service.model.dto.results;

public record PublishResultsResponseDTO(
        int requestedCount,
        int publishedCount,
        int unchangedCount
) {
}
