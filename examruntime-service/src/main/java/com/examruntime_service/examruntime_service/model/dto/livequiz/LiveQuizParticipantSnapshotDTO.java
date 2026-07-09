package com.examruntime_service.examruntime_service.model.dto.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record LiveQuizParticipantSnapshotDTO(
        UUID participantId,
        UUID studentId,
        String studentCode,
        String studentName,
        LiveQuizParticipantStatus status,
        int answeredCount,
        int totalQuestions,
        int correctCount,
        int wrongCount,
        int timeoutCount,
        BigDecimal totalScore,
        BigDecimal maxScore,
        Integer averageResponseMs,
        Integer currentRank,
        OffsetDateTime joinedAt,
        OffsetDateTime startedAt,
        OffsetDateTime finishedAt,
        OffsetDateTime lastSeenAt
) {
}
