package com.result_service.result_service.model.dto.results;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TeacherResultDetailDTO(
        TeacherResultRowDTO summary,
        String adjustmentReason,
        OffsetDateTime adjustedAt,
        UUID adjustedBy,
        List<TeacherResultAnswerDTO> answers,
        List<ResultIncidentDTO> incidents,
        List<ResultIncidentDTO> gradingErrors
) {
    public record TeacherResultAnswerDTO(
            UUID questionId,
            int questionOrder,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds,
            boolean correct,
            BigDecimal scoreAwarded,
            BigDecimal maxScore,
            String gradingNote,
            Object questionSnapshot,
            String answerState,
            QuestionDisplayDTO question,
            List<OptionDisplayDTO> options
    ) {
    }

    public record QuestionDisplayDTO(
            UUID questionId,
            String content,
            String type,
            String difficulty,
            String contentFormat
    ) {
    }

    public record OptionDisplayDTO(
            UUID optionId,
            String key,
            String content,
            String contentFormat,
            boolean selected,
            boolean correct
    ) {
    }

    public record ResultIncidentDTO(String type, String message, OffsetDateTime occurredAt) {
    }
}
