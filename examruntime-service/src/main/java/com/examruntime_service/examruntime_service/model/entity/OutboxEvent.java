package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_runtime_outbox_status_next_retry", columnList = "status,next_retry_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Outbox ben vung cho SubmissionCreated, SessionLocked va cac event runtime quan trong.
 */
public class OutboxEvent {

    @Id
    private UUID id;
    @Column(nullable = false, length = 64)
    private String aggregateType;
    @Column(nullable = false)
    private UUID aggregateId;
    @Column(nullable = false, length = 120)
    private String eventType;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    // Correlation/request/user metadata de trace event qua nhieu service.
    private String headers = "{}";
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status = OutboxStatus.PENDING;
    @Column(nullable = false)
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;
    @Column(columnDefinition = "text")
    private String lastError;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    @Version
    private Long version;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
