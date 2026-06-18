package com.question_service.question_service.model.dto.questions;

import com.question_service.question_service.model.entity.Difficulty;
import com.question_service.question_service.model.entity.QuestionStatus;
import com.question_service.question_service.model.entity.QuestionVisibility;

import java.time.LocalDateTime;
import java.util.UUID;

public record QuestionResponseDTO(
        UUID id,
        UUID subjectId,
        UUID topicId,
        UUID ownerTeacherId,
        String questionType,
        String content,
        Difficulty difficulty,
        Double defaultScore,
        int estimatedSecond,
        QuestionVisibility visibility,
        QuestionStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
