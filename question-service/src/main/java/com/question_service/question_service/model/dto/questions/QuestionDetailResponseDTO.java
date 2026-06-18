package com.question_service.question_service.model.dto.questions;

import com.question_service.question_service.model.entity.ContentFormat;
import com.question_service.question_service.model.entity.Difficulty;
import com.question_service.question_service.model.entity.QuestionStatus;
import com.question_service.question_service.model.entity.QuestionVisibility;
import com.question_service.question_service.model.entity.Source;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record QuestionDetailResponseDTO(
        UUID id,
        UUID subjectId,
        UUID topicId,
        UUID ownerTeacherId,
        String questionType,
        String content,
        ContentFormat contentFormat,
        String explanation,
        Difficulty difficulty,
        Double defaultScore,
        int estimatedSecond,
        QuestionVisibility visibility,
        QuestionStatus status,
        Source source,
        List<QuestionOptionResponseDTO> options,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
