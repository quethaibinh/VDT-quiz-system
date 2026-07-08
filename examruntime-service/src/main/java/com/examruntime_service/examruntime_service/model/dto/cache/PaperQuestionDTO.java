package com.examruntime_service.examruntime_service.model.dto.cache;

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
        Integer timeLimitSeconds,
        String imageObjectKey,
        List<PaperOptionDTO> options
) {
    public PaperQuestionDTO(
            UUID questionId,
            long questionVersion,
            String difficulty,
            String type,
            String content,
            String contentFormat,
            double score,
            Integer timeLimitSeconds,
            List<PaperOptionDTO> options
    ) {
        this(questionId, questionVersion, difficulty, type, content, contentFormat, score, timeLimitSeconds, null, options);
    }

    public PaperQuestionDTO(
            UUID questionId,
            long questionVersion,
            String difficulty,
            String type,
            String content,
            String contentFormat,
            double score,
            List<PaperOptionDTO> options
    ) {
        this(questionId, questionVersion, difficulty, type, content, contentFormat, score, null, null, options);
    }
}
