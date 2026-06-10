package com.question_service.question_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "subject")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Subject extends BaseEntity{

    @Column(name = "code", unique = true, nullable = false)
    private String code;
    @Column(name = "name", nullable = false)
    private String name;
    private String description;
    private UUID createdByAdminId;
    private UUID updatedByAdminId;
    @Column(name = "status", nullable = false)
    private String status;

}
