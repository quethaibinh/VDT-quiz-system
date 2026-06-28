package com.result_service.result_service.service.results;

import java.util.UUID;

public record StudentSnapshotInfo(
        UUID studentId,
        String studentCode,
        String studentName
) {
}
