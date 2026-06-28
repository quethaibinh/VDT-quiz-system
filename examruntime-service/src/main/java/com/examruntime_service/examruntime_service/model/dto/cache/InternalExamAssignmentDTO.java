package com.examruntime_service.examruntime_service.model.dto.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
// DTO nhan thong tin phan cong cua mot ca thi tu internal api cua exam-service
public class InternalExamAssignmentDTO {
    private UUID examId;
    private List<AssignmentDetail> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    // Chi tiet tung luot phan cong
    public static class AssignmentDetail {
        private UUID assignmentId;
        private UUID studentId;
    }
}
