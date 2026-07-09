package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "live_quiz_participants",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_live_quiz_participant_student", columnNames = {"room_id", "student_id"})
        },
        indexes = {
                @Index(name = "idx_live_quiz_participants_room_score", columnList = "room_id,total_score"),
                @Index(name = "idx_live_quiz_participants_room_status", columnList = "room_id,status")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuizParticipant extends BaseEntity { // bang này luu nhung thong tin khi tham gia choi cua sinh vien

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "student_id", nullable = false)
    private UUID studentId;

    @Column(name = "student_code_snapshot", length = 64)
    private String studentCodeSnapshot;

    @Column(name = "student_name_snapshot", nullable = false)
    private String studentNameSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LiveQuizParticipantStatus status = LiveQuizParticipantStatus.JOINED;

    @Column(nullable = false)
    private int currentQuestionPosition;

    private UUID currentQuestionId;
    private OffsetDateTime currentQuestionStartedAt;
    private OffsetDateTime currentQuestionEndsAt;

    @Column(nullable = false)
    private int totalQuestions;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal totalScore = BigDecimal.ZERO;

    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal maxScore = BigDecimal.ZERO;

    @Column(nullable = false)
    private int correctCount;

    @Column(nullable = false)
    private int wrongCount;

    @Column(nullable = false)
    private int timeoutCount;

    @Column(nullable = false)
    private int answeredCount;

    private Integer averageResponseMs;
    private Integer currentRank;
    private OffsetDateTime joinedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    private OffsetDateTime lastSeenAt;

    @Version
    private long version;
}
