package com.result_service.result_service.model.dto.results;

import java.util.List;
import java.util.UUID;

public record PublishResultsRequestDTO(
        List<UUID> resultIds
) {
}
