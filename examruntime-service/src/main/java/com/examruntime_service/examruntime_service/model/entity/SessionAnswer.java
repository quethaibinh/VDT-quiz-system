package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.AnswerSource;
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
@Table(name = "session_answers",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_runtime_answer_session_question", columnNames = {"session_id", "question_id"})
        },
        indexes = {
                @Index(name = "idx_runtime_answer_exam_student", columnList = "exam_id,student_id")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * Checkpoint ben vung cua cau tra loi.
 * Runtime van ghi nong vao Redis; bang nay chi flush dinh ky hoac khi submit.
 */
public class SessionAnswer extends BaseEntity {

    @Column(nullable = false)
    private UUID sessionId;
    @Column(nullable = false)
    private UUID examId;
    @Column(nullable = false)
    private UUID studentId;
    @Column(nullable = false)
    private UUID questionId;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String selectedOptionIds = "[]";
    @Column(columnDefinition = "text")
    private String answerText;
    @Column(name = "is_marked_for_review")
    private boolean markedForReview;
    private OffsetDateTime answeredAt;
    @Column(nullable = false)
    private OffsetDateTime lastChangedAt;
    @Column(nullable = false)
    // Sequence tu client de bo qua autosave den muon/out-of-order.
    private long clientSeq;
    @Column(nullable = false)
    // Sequence do server cap de debug thu tu ghi nhan.
    private long serverSeq;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AnswerSource source = AnswerSource.AUTOSAVE;
}
