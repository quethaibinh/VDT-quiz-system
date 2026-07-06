package com.examruntime_service.examruntime_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "live_quiz_participant_papers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_live_quiz_participant_paper", columnNames = "participant_id")
        },
        indexes = {
                @Index(name = "idx_live_quiz_papers_room_student", columnList = "room_id,student_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuizParticipantPaper extends BaseEntity { // bang nay luu ma tron cau hoi va danh sach id cac cau hoi cua sinh vien tham gia choi

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(nullable = false)
    private long seed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_order", nullable = false, columnDefinition = "jsonb")
    private String questionOrder = "[]";
}
