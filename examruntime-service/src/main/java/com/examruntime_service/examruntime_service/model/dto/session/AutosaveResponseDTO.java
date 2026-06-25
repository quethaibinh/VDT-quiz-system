package com.examruntime_service.examruntime_service.model.dto.session;

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
// DTO tra ve ket qua sau khi tu dong luu thanh cong lam batcheable checkpoint
public class AutosaveResponseDTO {
    private UUID sessionId;
    private long acceptedSeq;
    private long serverSeq;
    private int savedCount;
    private int skippedCount;
    private OffsetDateTime lastAutosaveAt;
}
