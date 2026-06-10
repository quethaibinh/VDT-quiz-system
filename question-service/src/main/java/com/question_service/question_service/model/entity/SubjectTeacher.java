package com.question_service.question_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "subject_teacher",
        uniqueConstraints = @UniqueConstraint(columnNames = {"subject_id", "teacher_id"})
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectTeacher extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;

    @Column(name = "teacher_id", nullable = false)
    private UUID teacherId;

    @Column(name = "status", nullable = false)
    private String status;

    private UUID assignedByAdminId;
    private LocalDateTime assignedAt;

}
