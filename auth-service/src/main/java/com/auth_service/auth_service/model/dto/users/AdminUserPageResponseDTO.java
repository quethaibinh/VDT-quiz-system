package com.auth_service.auth_service.model.dto.users;

import org.springframework.data.domain.Page;

import java.util.List;

public record AdminUserPageResponseDTO(
        List<AdminUserSummaryDTO> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean first,
        boolean last
) {
    public static AdminUserPageResponseDTO from(Page<AdminUserSummaryDTO> page) {
        return new AdminUserPageResponseDTO(
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
