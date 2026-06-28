package com.exam_service.exam_service.model.dto.exams;

import org.springframework.data.domain.Page;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * DTO danh sach ca thi cua hoc sinh kem thoi gian may chu.
 */
public record StudentExamPageDTO(
        OffsetDateTime serverTime,
        List<StudentExamSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static StudentExamPageDTO from(Page<StudentExamSummaryDTO> page, OffsetDateTime serverTime) {
        return new StudentExamPageDTO(
                serverTime,
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
