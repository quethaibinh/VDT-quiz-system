package com.result_service.result_service.model.dto.results;

public enum ResultVisibilityStateDTO {
    GRADING,
    READY,
    PENDING_REVIEW,
    RELEASED,
    LOCKED_UNTIL_CLOSED,
    GRADING_FAILED,
    CONFIG_MISSING
}
