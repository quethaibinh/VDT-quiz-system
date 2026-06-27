package com.result_service.result_service.model.dto.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record SubmissionCreatedEvent(
        UUID eventId,
        String eventType,
        String producer,
        OffsetDateTime occurredAt,
        UUID submissionId,
        UUID sessionId,
        UUID examId,
        UUID studentId,
        int attemptNo,
        int snapshotVersion,
        String submitReason,
        OffsetDateTime submittedAt,
        List<AnswerSnapshotItem> answerSnapshot,
        PaperSnapshot paperSnapshot
) {
    public static final String EVENT_TYPE = "SubmissionCreated";

    public record AnswerSnapshotItem(
            UUID questionId,
            List<UUID> selectedOptionIds
    ) {
    }

    public record PaperSnapshot(
            List<QuestionSnapshot> questions,
            Object studentSnapshot,
            Object examSnapshot
    ) {
    }

    public record QuestionSnapshot(
            UUID questionId,
            Integer questionOrder,
            List<UUID> correctOptionIds,
            BigDecimal maxScore,
            BigDecimal score,
            Object questionSnapshot
    ) {
        public BigDecimal effectiveMaxScore() {
            if (maxScore != null) {
                return maxScore;
            }
            if (score != null) {
                return score;
            }
            return BigDecimal.ONE;
        }
    }
}
