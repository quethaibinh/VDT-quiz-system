package com.examruntime_service.examruntime_service.model.dto.monitor;

import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringSeverity;
import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MonitorEventDTO(
        UUID id,
        UUID examId,
        UUID sessionId,
        UUID studentId,
        ProctoringEventType eventType,
        ProctoringSeverity severity,
        OffsetDateTime occurredAt,
        OffsetDateTime receivedAt,
        String metadata,
        Integer countInSession
) {
}
