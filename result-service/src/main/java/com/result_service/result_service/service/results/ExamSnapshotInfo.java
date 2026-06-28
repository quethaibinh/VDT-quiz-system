package com.result_service.result_service.service.results;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ExamSnapshotInfo(
        UUID examId,
        int snapshotVersion,
        String code,
        String title,
        UUID subjectId,
        String subjectName,
        UUID ownerTeacherId,
        OffsetDateTime startAt,
        OffsetDateTime endAt,
        String showResultPolicy
) {
}
