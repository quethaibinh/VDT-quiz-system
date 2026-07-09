package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam_sessions",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_runtime_session_exam_student_attempt", columnNames = {"exam_id", "student_id", "attempt_no"})
        },
        indexes = {
                @Index(name = "idx_runtime_session_exam_status", columnList = "exam_id,status"),
                @Index(name = "idx_runtime_session_student_status", columnList = "student_id,status")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * Phien lam bai cua mot hoc sinh trong mot ca thi.
 * Session khong phai ma de; cau truc de ca nhan nam o questionOrder/optionOrders.
 */
public class ExamSession extends BaseEntity {

    @Column(nullable = false)
    private UUID examId;
    @Column(nullable = false)
    private UUID studentId;
    private UUID assignmentId;
    @Column(nullable = false)
    private int snapshotVersion;
    @Column(nullable = false)
    private int attemptNo = 1;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamSessionStatus status = ExamSessionStatus.CREATED;
    private OffsetDateTime serverStartedAt;
    private OffsetDateTime serverDeadlineAt;
    private OffsetDateTime lastSeenAt;
    private OffsetDateTime lastAutosaveAt;
    private OffsetDateTime submittedAt;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private SubmitReason submitReason;
    @Column(length = 64)
    private String clientTimezone;
    private String startedIp;
    private String lastIp;
    @Column(columnDefinition = "text")
    private String userAgent;
    // Seed dung de debug/replay cach sinh de ca nhan, khong thay the snapshot thu tu de.
    private Long paperSeed;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Danh sach question_id theo thu tu hoc sinh da nhin thay.
    private String questionOrder = "[]";
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Map question_id -> danh sach option_id sau khi shuffle.
    private String optionOrders = "{}";
    @Column(nullable = false)
    private long autosaveSeq;
    @Column(nullable = false)
    private int answeredCount;
    @Column(nullable = false)
    private int violationCount;
    @Column(columnDefinition = "text")
    private String lockedReason;
    @Version
    // Bao ve session khi autosave, submit va lock cung cap nhat gan nhau.
    private long version;
}
