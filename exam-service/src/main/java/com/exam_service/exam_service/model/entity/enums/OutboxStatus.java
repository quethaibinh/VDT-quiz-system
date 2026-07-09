package com.exam_service.exam_service.model.entity.enums;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    EXPIRED
}
