package com.result_service.result_service.model.dto.events;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Ban mirror cua event Runtime gui sang Result Service.
// String enum duoc dung cho status de consumer ben Result Service khong bi vo neu enum Runtime thay doi nhe.
public record LiveQuizRoomClosedEvent(
        UUID eventId,
        String eventType,
        String producer,
        OffsetDateTime occurredAt,
        UUID roomId,
        UUID examId,
        UUID ownerTeacherId,
        String roomCode,
        String quizTitle,
        UUID subjectId,
        String subjectName,
        int snapshotVersion,
        int questionCount,
        OffsetDateTime closedAt,
        List<ParticipantResult> participants
) {
    public static final String EVENT_TYPE = "LiveQuizRoomClosed";

    // Row ket qua cua mot participant da duoc Runtime tinh chot.
    // Result Service khong tinh lai rank, chi luu va expose theo quyen teacher/student.
    public record ParticipantResult(
            UUID participantId,
            UUID studentId,
            String studentCodeSnapshot,
            String studentNameSnapshot,
            int finalRank,
            String status,
            int answeredCount,
            int correctCount,
            int wrongCount,
            int timeoutCount,
            int notReachedCount,
            BigDecimal totalScore,
            BigDecimal maxScore,
            Integer averageResponseMs,
            OffsetDateTime joinedAt,
            OffsetDateTime startedAt,
            OffsetDateTime finishedAt,
            List<AnswerResult> answers
    ) {
    }

    // Answer detail de luu vao result_answers cho giao vien xem lai tung cau.
    public record AnswerResult(
            UUID questionId,
            int questionPosition,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds,
            boolean correct,
            BigDecimal scoreAwarded,
            BigDecimal maxScore,
            String answerStatus,
            Integer responseTimeMs,
            OffsetDateTime answeredAt,
            OffsetDateTime questionStartedAt,
            OffsetDateTime questionEndsAt,
            OffsetDateTime serverReceivedAt,
            QuestionSnapshot questionSnapshot
    ) {
    }

    // Snapshot cau hoi kem theo event, giup Result Service doc doc lap voi question-service.
    public record QuestionSnapshot(
            UUID questionId,
            long questionVersion,
            String difficulty,
            String type,
            String content,
            String contentFormat,
            double score,
            Integer timeLimitSeconds,
            String imageObjectKey,
            List<OptionSnapshot> options
    ) {
    }

    public record OptionSnapshot(
            UUID optionId,
            String key,
            String content,
            String contentFormat
    ) {
    }
}
