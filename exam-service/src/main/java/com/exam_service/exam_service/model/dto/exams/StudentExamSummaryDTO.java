package com.exam_service.exam_service.model.dto.exams;

import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO chua thong tin tom tat cua ca thi phan cho hoc sinh.
 */
public record StudentExamSummaryDTO(
        UUID examId,
        String code,
        String title,
        UUID subjectId,
        String subjectName,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        int durationMinutes,
        int questionCount,
        ExamStatus status,
        StudentExamAvailability studentAvailability,
        AssignmentStatus assignmentStatus
) {
}
