package com.auth_service.auth_service.model.dto.students;

import org.springframework.data.domain.Page;

import java.util.List;

public record StudentPageResponseDTO(
        List<StudentSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static StudentPageResponseDTO from(Page<StudentSummaryDTO> page) {
        return new StudentPageResponseDTO(
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
