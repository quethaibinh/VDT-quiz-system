package com.question_service.question_service.model.dto.subjects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectResponseDTO {

    private UUID id;
    private String code;
    private String name;
    private String description;
    private String status;
    private UUID createdByAdminId;
    private UUID updatedByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime assignedAt;

}
