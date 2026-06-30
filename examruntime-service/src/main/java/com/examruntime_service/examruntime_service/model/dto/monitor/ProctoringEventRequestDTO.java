package com.examruntime_service.examruntime_service.model.dto.monitor;

import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class ProctoringEventRequestDTO {

    @NotBlank
    private String clientEventId;
    @NotNull
    private ProctoringEventType eventType;
    @NotNull
    private OffsetDateTime occurredAt;
    private String metadata = "{}";
    private Integer durationMs;
}
