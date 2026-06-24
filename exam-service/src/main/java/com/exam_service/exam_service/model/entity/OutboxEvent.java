package com.exam_service.exam_service.model.entity;

import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "outbox_events", indexes = {
        @Index(name = "idx_outbox_status_next_retry", columnList = "status,next_retry_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Cong viec ben vung de day snapshot tu PostgreSQL sang Redis.
 * Payload chi giu ID va version, khong sao chep noi dung de thi.
 */
public class OutboxEvent {

    @jakarta.persistence.Id
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
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private OutboxStatus status = OutboxStatus.PENDING;
    private int retryCount;
    private LocalDateTime nextRetryAt;
    private LocalDateTime publishedAt;
    @Column(columnDefinition = "text")
    private String lastError;

    @jakarta.persistence.Version
    private Long version;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @jakarta.persistence.PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @jakarta.persistence.PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
