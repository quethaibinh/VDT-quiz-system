package com.examruntime_service.examruntime_service.model.dto.livequiz;

import java.util.UUID;

public record LiveQuizSelectedOptionResultDTO(
        UUID optionId,
        LiveQuizSelectedOptionResult result
) {
}
