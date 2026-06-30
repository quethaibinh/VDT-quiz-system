package com.examruntime_service.examruntime_service.model.dto.monitor;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record StudentAlertDTO(
        UUID examId,
        UUID sessionId,
        String type,
        String message,
        OffsetDateTime occurredAt
) {
}
