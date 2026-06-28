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
// DTO chua dap an hoc sinh da chon de phuc vu cho api resume hoac autosave
public class StudentAnswerDTO {
    private UUID questionId;
    private List<UUID> selectedOptionIds;
    private String answerText;
    private boolean markedForReview;
}
