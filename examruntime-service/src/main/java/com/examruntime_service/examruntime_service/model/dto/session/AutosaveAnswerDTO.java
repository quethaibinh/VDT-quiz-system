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
// Chi tiet tung cau tra loi trong yeu cau autosave
public class AutosaveAnswerDTO {
    private UUID questionId;
    private List<UUID> selectedOptionIds;
    private String answerText;
    private boolean markedForReview;
}
