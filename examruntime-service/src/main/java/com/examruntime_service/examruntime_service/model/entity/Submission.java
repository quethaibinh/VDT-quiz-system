package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.SubmissionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "submissions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_runtime_submission_session", columnNames = "session_id"),
                @UniqueConstraint(name = "uk_runtime_submission_exam_student_attempt", columnNames = {"exam_id", "student_id", "attempt_no"}),
                @UniqueConstraint(name = "uk_runtime_submission_idempotency_key", columnNames = "idempotency_key"),
                @UniqueConstraint(name = "uk_runtime_submission_message_id", columnNames = "message_id")
        },
        indexes = {
                @Index(name = "idx_runtime_submission_exam_status", columnList = "exam_id,status")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * Bien nhan da thu bai.
 * Result Service cham diem tu event SubmissionCreated, khong ghi truc tiep bang nay.
 */
public class Submission extends BaseEntity {

    @Column(nullable = false)
    private UUID sessionId;
    @Column(nullable = false)
    private UUID examId;
    @Column(nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private int attemptNo;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmissionStatus status = SubmissionStatus.RECEIVED;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubmitReason submitReason = SubmitReason.STUDENT;
    @Column(nullable = false)
    private OffsetDateTime submittedAt;
    @Column(nullable = false)
    private OffsetDateTime receivedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Snapshot cau tra loi tai thoi diem thu bai, dung cho cham diem va audit.
    private String answerSnapshot = "[]";
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Thu tu cau hoi/options hoc sinh da nhin thay, giup review/phuc khao khong phu thuoc lai vao thuat toan shuffle.
    private String paperSnapshot = "{}";
    @Column(nullable = false)
    private int answerCount;
    @Column(length = 128)
    // Khoa chong submit trung khi frontend retry hoac hoc sinh bam nut nhieu lan.
    private String idempotencyKey;
    private UUID messageId;
    private OffsetDateTime queuedAt;
    private OffsetDateTime gradedAt;
    @Column(columnDefinition = "text")
    private String failureReason;
}
