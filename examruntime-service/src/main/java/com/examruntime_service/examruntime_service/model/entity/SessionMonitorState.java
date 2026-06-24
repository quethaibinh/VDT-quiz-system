package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "session_monitor_states",
        indexes = {
                @Index(name = "idx_runtime_monitor_exam_risk", columnList = "exam_id,risk_level"),
                @Index(name = "idx_runtime_monitor_exam_online", columnList = "exam_id,online_status")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Snapshot ben vung cho dashboard giam sat.
 * Redis van la read path realtime; bang nay giup khoi phuc sau restart.
 */
public class SessionMonitorState {

    @Id
    private UUID sessionId;
    @Column(nullable = false)
    private UUID examId;
    @Column(nullable = false)
    private UUID studentId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MonitorOnlineStatus onlineStatus = MonitorOnlineStatus.UNKNOWN;
    private OffsetDateTime lastHeartbeatAt;
    private OffsetDateTime lastEventAt;
    @Column(nullable = false)
    private int answeredCount;
    @Column(nullable = false)
    private int totalQuestions;
    @Column(nullable = false)
    private int tabHiddenCount;
    @Column(nullable = false)
    private int windowBlurCount;
    @Column(nullable = false)
    private int fullscreenExitCount;
    @Column(nullable = false)
    private int offlineCount;
    @Column(nullable = false)
    private int totalViolationCount;
    @Column(nullable = false, precision = 8, scale = 3)
    // Diem rui ro tong hop; khong phai moi tin hieu deu khoa bai truc tiep.
    private BigDecimal riskScore = BigDecimal.ZERO;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private RiskLevel riskLevel = RiskLevel.LOW;
    @Column(name = "is_locked")
    private boolean locked;
    @Column(columnDefinition = "text")
    private String lockedReason;
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void touch() {
        updatedAt = LocalDateTime.now();
    }
}
