package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.HandledStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringSeverity;
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
@Table(name = "proctoring_events",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_runtime_proctoring_session_client_event", columnNames = {"session_id", "client_event_id"})
        },
        indexes = {
                @Index(name = "idx_runtime_proctoring_exam_occurred", columnList = "exam_id,occurred_at"),
                @Index(name = "idx_runtime_proctoring_session_type_occurred", columnList = "session_id,event_type,occurred_at")
        })
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
/**
 * Event giam sat append-only.
 * Heartbeat binh thuong khong nhat thiet ghi vao bang nay de tranh tai DB.
 */
public class ProctoringEvent extends BaseEntity {

    @Column(nullable = false)
    private UUID examId;
    @Column(nullable = false)
    private UUID sessionId;
    @Column(nullable = false)
    private UUID studentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ProctoringEventType eventType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ProctoringSeverity severity = ProctoringSeverity.INFO;
    @Column(nullable = false)
    private OffsetDateTime occurredAt;
    @Column(nullable = false)
    private OffsetDateTime receivedAt;
    @Column(length = 128)
    // ID do frontend tao de chong ghi trung khi retry WebSocket/REST fallback.
    private String clientEventId;
    private Integer durationMs;
    private Integer countInSession;
    private String ipAddress;
    @Column(columnDefinition = "text")
    private String userAgent;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Chua visibility state, fullscreen state, screen size, client instance id.
    private String metadata = "{}";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private HandledStatus handledStatus = HandledStatus.OPEN;
    private UUID handledBy;
    private OffsetDateTime handledAt;
}
