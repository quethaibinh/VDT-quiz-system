package com.exam_service.exam_service.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "exam")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam extends BaseEntity{

    private String code;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectNameSnapshot;
    private UUID createdByTeacherId;
    private 

}
