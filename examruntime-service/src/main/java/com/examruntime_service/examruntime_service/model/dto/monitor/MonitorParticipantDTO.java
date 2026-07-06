package com.examruntime_service.examruntime_service.model.dto.monitor;

import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MonitorParticipantDTO(
        UUID sessionId,
        UUID studentId,
        String studentCode,
        String studentName,
        String status,
        int answeredCount,
        int totalQuestions,
        int totalViolationCount,
        BigDecimal riskScore,
        RiskLevel riskLevel,
        boolean locked,
        OffsetDateTime lastSeenAt,
        OffsetDateTime lastHeartbeatAt,
        OffsetDateTime lastEventAt
) {
}
