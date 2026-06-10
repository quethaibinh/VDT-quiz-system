package com.question_service.question_service.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "import_job")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportJob extends BaseEntity{

    private UUID uploadBy;
    private UUID subjectId;
    private String fileName;
    @Enumerated(EnumType.STRING)
    private ImportStatus importStatus;
    private Double fileSize;
    private int createdQuestionCount;
    private String fileStorage;

}
