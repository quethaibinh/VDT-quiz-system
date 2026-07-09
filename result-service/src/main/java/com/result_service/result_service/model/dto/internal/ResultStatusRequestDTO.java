package com.result_service.result_service.model.dto.internal;

import java.util.List;
import java.util.UUID;

public class ResultStatusRequestDTO {

    private List<UUID> submissionIds;

    public ResultStatusRequestDTO() {
    }

    public ResultStatusRequestDTO(List<UUID> submissionIds) {
        this.submissionIds = submissionIds;
    }

    public List<UUID> getSubmissionIds() {
        return submissionIds;
    }

    public void setSubmissionIds(List<UUID> submissionIds) {
        this.submissionIds = submissionIds;
    }
}
