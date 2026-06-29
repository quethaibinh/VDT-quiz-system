package com.result_service.result_service.model.dto.internal;

import java.util.List;
import java.util.UUID;

public class ResultStatusResponseDTO {

    private List<UUID> gradedSubmissionIds;

    public ResultStatusResponseDTO() {
    }

    public ResultStatusResponseDTO(List<UUID> gradedSubmissionIds) {
        this.gradedSubmissionIds = gradedSubmissionIds;
    }

    public List<UUID> getGradedSubmissionIds() {
        return gradedSubmissionIds;
    }

    public void setGradedSubmissionIds(List<UUID> gradedSubmissionIds) {
        this.gradedSubmissionIds = gradedSubmissionIds;
    }
}
