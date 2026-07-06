package com.result_service.result_service.service.livequiz;

import java.time.OffsetDateTime;
import java.util.UUID;

record LiveQuizResultSnapshot(
        String roomCode,
        String quizTitle,
        UUID subjectId,
        String subjectName,
        UUID ownerTeacherId,
        OffsetDateTime closedAt,
        int finalRank,
        int participantCount,
        int timeoutCount,
        int notReachedCount
) {
}
