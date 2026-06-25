package com.examruntime_service.examruntime_service.model.dto.session;

import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO tra ve de ca nhan va thoi gian lam bai sau khi bat dau/resume
public class StudentPaperResponseDTO {
    private UUID sessionId;
    private ExamSessionStatus status;
    private OffsetDateTime serverStartedAt;
    private OffsetDateTime serverDeadlineAt;
    private List<StudentQuestionDTO> questions;
    private List<StudentAnswerDTO> answers;
}
