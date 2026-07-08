package com.examruntime_service.examruntime_service.model.dto.session;

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
// DTO question an toan cho hoc sinh (khong chua dap an dung)
public class StudentQuestionDTO {
    private UUID questionId;
    private String difficulty;
    private String type;
    private String content;
    private String contentFormat;
    private double score;
    private String imageObjectKey;
    private String imageUrl;
    private List<StudentOptionDTO> options;
}
