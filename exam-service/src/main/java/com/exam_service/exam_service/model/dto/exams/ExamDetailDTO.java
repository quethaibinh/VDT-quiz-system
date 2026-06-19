package com.exam_service.exam_service.model.dto.exams;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.HandleViolation;
import com.exam_service.exam_service.model.entity.enums.ShowResultPolicy;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamDetailDTO(
        UUID id,
        String code,
        String title,
        String description,
        UUID subjectId,
        String subjectName,
        UUID collectionId,
        String collectionName,
        int easyCount,
        int mediumCount,
        int hardCount,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int durationMinutes,
        int joinBeforeMinutes,
        int joinAfterMinutes,
        boolean shuffleQuestions,
        boolean shuffleOptions,
        ShowResultPolicy showResultPolicy,
        boolean autoSubmit,
        boolean requireFullscreen,
        int maxViolationAllowed,
        HandleViolation handleViolation,
        long assignedCount,
        ExamStatus status,
        long version
) {
}
