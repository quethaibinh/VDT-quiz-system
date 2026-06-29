package com.result_service.result_service.client;

import java.util.UUID;

public record PaperOptionDTO(
        UUID optionId,
        String key,
        String content,
        String contentFormat
) {
}
