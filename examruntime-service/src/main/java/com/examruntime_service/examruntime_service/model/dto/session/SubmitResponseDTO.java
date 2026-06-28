package com.examruntime_service.examruntime_service.model.dto.session;

import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// Bien nhan nop bai tra ve cho student API.
public class SubmitResponseDTO {

    private UUID submissionId;
    private UUID sessionId;
    private String status;
    private SubmitReason submitReason;
    private OffsetDateTime submittedAt;
}
