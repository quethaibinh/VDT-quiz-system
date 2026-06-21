package com.exam_service.exam_service.model.entity;

import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_assignments",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_exam_assignment_exam_student",
                    columnNames = {"exam_id", "student_id"}
            )
        },
        indexes = {
            @Index(name = "idx_student_id_status", columnList = "student_id,status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Lien ket mot hoc sinh voi ca thi va giu snapshot danh tinh luc phan cong.
 * Unique constraint dam bao moi hoc sinh chi co mot dong lich su trong mot ca thi.
 */
public class ExamAssignment extends BaseEntity{

    private UUID examId;
    private UUID studentId;
    // Snapshot giup hien thi lich su ngay ca khi ho so Auth thay doi.
    private String studentCodeSnapshot;
    private String studentNameSnapshot;
    @Enumerated(EnumType.STRING)
    private AssignmentStatus status = AssignmentStatus.ASSIGNED;
    private UUID assignedBy;
    private LocalDateTime assignedAt;
    private LocalDateTime removedAt;
    private String note;

}
