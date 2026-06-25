package com.exam_service.exam_service.model.dto.assignments;

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
// DTO chua danh sach thong tin phan cong cua mot ky thi (exam)
public class InternalExamAssignmentDTO {
    private UUID examId;
    private List<AssignmentDetail> assignments;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    // Chi tiet tung luot phan cong cho hoc sinh
    public static class AssignmentDetail {
        private UUID assignmentId;
        private UUID studentId;
    }
}
