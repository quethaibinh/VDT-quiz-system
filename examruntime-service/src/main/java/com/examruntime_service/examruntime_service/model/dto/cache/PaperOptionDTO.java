package com.examruntime_service.examruntime_service.model.dto.cache;

import java.util.UUID;

public record PaperOptionDTO(
        UUID optionId,
        String key,
        String content,
        String contentFormat
) {
}
