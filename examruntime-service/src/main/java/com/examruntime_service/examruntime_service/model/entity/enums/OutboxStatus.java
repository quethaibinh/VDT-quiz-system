package com.examruntime_service.examruntime_service.model.entity.enums;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    PUBLISHED,
    FAILED,
    EXPIRED
}
