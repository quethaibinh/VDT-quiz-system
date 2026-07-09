package com.examruntime_service.examruntime_service.model.dto.monitor;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record MonitorRealtimeMessageDTO(
        String messageType,
        UUID examId,
        UUID sessionId,
        UUID studentId,
        MonitorParticipantDTO participant,
        MonitorEventDTO event,
        StudentAlertDTO alert,
        String instanceId,
        OffsetDateTime occurredAt
) {
}
