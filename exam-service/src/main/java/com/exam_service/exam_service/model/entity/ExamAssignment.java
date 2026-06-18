package com.exam_service.exam_service.model.entity;

import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_assignment",
        indexes = {
            @Index(name = "idx_student_id_status", columnList = "student_id,status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamAssignment extends BaseEntity{

    private UUID examId;
    private UUID studentId;
    private String studentCodeSnapshot;
    private String studentNameSnapshot;
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;
    private UUID assignedBy;
    private LocalDateTime assignedAt;
    private String note;

}
