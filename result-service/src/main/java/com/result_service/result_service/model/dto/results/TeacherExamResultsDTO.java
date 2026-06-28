package com.result_service.result_service.model.dto.results;

import com.result_service.result_service.model.dto.common.PageResponseDTO;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record TeacherExamResultsDTO(
        UUID examId,
        String code,
        String title,
        String subjectName,
        String showResultPolicy,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        long participantCount,
        long gradedCount,
        long releasedCount,
        long pendingReviewCount,
        BigDecimal average,
        BigDecimal highest,
        BigDecimal lowest,
        List<ScoreBucketDTO> distribution,
        PageResponseDTO<TeacherResultRowDTO> students
) {
    public record ScoreBucketDTO(String range, long count) {
    }
}
