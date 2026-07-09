package com.examruntime_service.examruntime_service.model.dto.session;

import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
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
// DTO phan hoi khi hoc sinh join vao ca thi thanh cong
public class StudentJoinResponseDTO {
    private UUID sessionId;
    private UUID examId;
    private ExamSessionStatus status;
    private OffsetDateTime serverTime;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private boolean canStart;
    private long remainingSecondsToStart;
}
