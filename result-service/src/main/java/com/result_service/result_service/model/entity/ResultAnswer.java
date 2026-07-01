package com.result_service.result_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
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
        name = "result_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_result_answers_result_question", columnNames = {"result_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_result_answers_exam_question_correct", columnList = "exam_id,question_id,is_correct")
        }
)
public class ResultAnswer extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "result_id", nullable = false)
    private ExamResult result;

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "question_order", nullable = false)
    private int questionOrder;

    @Column(name = "question_position")
    private Integer questionPosition;

    @Lob
    @Column(name = "selected_option_ids", nullable = false)
    private String selectedOptionIds;

    @Lob
    @Column(name = "correct_option_ids", nullable = false)
    private String correctOptionIds;

    @Column(name = "is_correct", nullable = false)
    private boolean correct;

    @Column(name = "score_awarded", nullable = false, precision = 8, scale = 3)
    private BigDecimal scoreAwarded;

    @Column(name = "max_score", nullable = false, precision = 8, scale = 3)
    private BigDecimal maxScore;

    @Column(name = "grading_note", columnDefinition = "text")
    private String gradingNote;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(name = "answered_at")
    private OffsetDateTime answeredAt;

    @Column(name = "answer_status", length = 32)
    private String answerStatus;

    @Lob
    @Column(name = "question_snapshot")
    private String questionSnapshot;
}
