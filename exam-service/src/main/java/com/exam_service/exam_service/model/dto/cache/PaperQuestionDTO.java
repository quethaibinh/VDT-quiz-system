package com.exam_service.exam_service.model.dto.cache;

import java.util.List;
import java.util.UUID;

public record PaperQuestionDTO(
        UUID questionId,
        long questionVersion,
        String difficulty,
        String type,
        String content,
        String contentFormat,
        double score,
        List<PaperOptionDTO> options
) {
}
