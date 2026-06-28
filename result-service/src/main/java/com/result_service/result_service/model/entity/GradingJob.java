package com.result_service.result_service.model.entity;

import com.result_service.result_service.model.entity.enums.GradingJobStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "grading_jobs", uniqueConstraints = {
        @UniqueConstraint(name = "uk_grading_jobs_message_id", columnNames = "message_id")
})
public class GradingJob extends BaseEntity {

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private GradingJobStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "locked_by", length = 120)
    private String lockedBy;

    @Column(name = "locked_at")
    private OffsetDateTime lockedAt;

    @Column(name = "started_at")
    private OffsetDateTime startedAt;

    @Column(name = "finished_at")
    private OffsetDateTime finishedAt;

    @Column(name = "error_code", length = 64)
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;
}
