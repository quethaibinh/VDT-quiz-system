package com.result_service.result_service.model.entity;

import com.result_service.result_service.model.entity.enums.ExamStatus;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(
        name = "exam_results",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_exam_results_submission_id", columnNames = "submission_id"),
                @UniqueConstraint(name = "uk_exam_results_exam_student_attempt", columnNames = {"exam_id", "student_id", "attempt_no"})
        },
        indexes = {
                @Index(name = "idx_exam_results_exam_score", columnList = "exam_id,total_score"),
                @Index(name = "idx_exam_results_student_graded", columnList = "student_id,graded_at")
        }
)
public class ExamResult extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "submission_id", nullable = false)
    private UUID submissionId;

    @Column(name = "attempt_no", nullable = false)
    private int attemptNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private ExamStatus status;

    @Column(name = "total_questions", nullable = false)
    private int totalQuestions;

    @Column(name = "answered_questions", nullable = false)
    private int answeredQuestions;

    @Column(name = "correct_count", nullable = false)
    private int correctCount;

    @Column(name = "wrong_count", nullable = false)
    private int wrongCount;

    @Column(name = "blank_count", nullable = false)
    private int blankCount;

    @Column(name = "total_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal totalScore;

    @Column(name = "adjusted_score", precision = 10, scale = 4)
    private BigDecimal adjustedScore;

    @Column(name = "adjustment_reason", columnDefinition = "text")
    private String adjustmentReason;

    @Column(name = "adjusted_at")
    private OffsetDateTime adjustedAt;

    @Column(name = "adjusted_by")
    private UUID adjustedBy;

    @Column(name = "max_score", nullable = false, precision = 10, scale = 4)
    private BigDecimal maxScore;

    @Column(name = "percentage", nullable = false, precision = 6, scale = 3)
    private BigDecimal percentage;

    @Column(name = "grade_label", length = 32)
    private String gradeLabel;

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "graded_at", nullable = false)
    private OffsetDateTime gradedAt;

    @Column(name = "grading_duration_ms")
    private Integer gradingDurationMs;

    @Column(name = "grader_version", nullable = false, length = 64)
    private String graderVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 32)
    private ResultReviewStatus reviewStatus = ResultReviewStatus.PENDING_REVIEW;

    @Column(name = "released_at")
    private OffsetDateTime releasedAt;

    @Column(name = "released_by")
    private UUID releasedBy;

    @Lob
    @Column(name = "student_snapshot", nullable = false)
    private String studentSnapshot;

    @Lob
    @Column(name = "exam_snapshot", nullable = false)
    private String examSnapshot;
}
