package com.exam_service.exam_service.model.dto.exams;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamSummaryDTO(
        UUID id,
        String code,
        String title,
        UUID subjectId,
        String subjectName,
        UUID collectionId,
        String collectionName,
        OffsetDateTime startAt,
        int durationMinutes,
        int questionCount,
        long assignedCount,
        ExamStatus status,
        long version
) {
}
