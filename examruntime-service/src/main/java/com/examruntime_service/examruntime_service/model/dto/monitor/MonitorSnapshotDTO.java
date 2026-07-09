package com.examruntime_service.examruntime_service.model.dto.monitor;

import lombok.Builder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Builder
public record MonitorSnapshotDTO(
        UUID examId,
        OffsetDateTime serverTime,
        List<MonitorParticipantDTO> participants,
        List<MonitorEventDTO> events
) {
}
