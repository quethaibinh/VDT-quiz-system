package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "live_quiz_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_live_quiz_answer_question", columnNames = {"room_id", "participant_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_live_quiz_answers_participant_position", columnList = "room_id,participant_id,question_position"),
                @Index(name = "idx_live_quiz_answers_question", columnList = "room_id,question_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuizAnswer extends BaseEntity {

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "participant_id", nullable = false)
    private UUID participantId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(nullable = false)
    private int questionPosition;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "selected_option_ids", columnDefinition = "jsonb")
    private String selectedOptionIds;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LiveQuizAnswerStatus answerStatus;

    private OffsetDateTime answeredAt;

    @Column(nullable = false)
    private OffsetDateTime questionStartedAt;

    @Column(nullable = false)
    private OffsetDateTime questionEndsAt;

    private Integer responseTimeMs;

    @Column(nullable = false)
    private boolean correct;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal scoreAwarded = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal maxScore = BigDecimal.ZERO;

    @Column(nullable = false)
    private OffsetDateTime serverReceivedAt;
}
