package com.question_service.question_service.model.dto.questions;

import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.Difficulty;
import com.question_service.question_service.model.entity.enums.QuestionStatus;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import com.question_service.question_service.model.entity.enums.Source;

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
