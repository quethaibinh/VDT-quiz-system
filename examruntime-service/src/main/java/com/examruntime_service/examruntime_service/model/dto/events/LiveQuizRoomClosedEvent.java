package com.examruntime_service.examruntime_service.model.dto.events;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

// Event chot phong live quiz do examruntime-service phat ra khi giao vien bam close.
// Payload nay la "ban su that" cua ket qua live quiz: co rank, diem, cau tra loi va snapshot cau hoi tai luc close.
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
    public static final String PRODUCER = "examruntime-service";

    // Ket qua da chot cua tung hoc sinh trong phong.
    // finalRank da duoc Runtime tinh xong, Result Service chi persist va query lai.
    public record ParticipantResult(
            UUID participantId,
            UUID studentId,
            String studentCodeSnapshot,
            String studentNameSnapshot,
            int finalRank,
            LiveQuizParticipantStatus status,
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

    // Chi tiet tung cau trong bai live quiz cua hoc sinh.
    // Bao gom ca correctOptionIds vi event chi di noi bo sang Result Service cho teacher/audit.
    public record AnswerResult(
            UUID questionId,
            int questionPosition,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds,
            boolean correct,
            BigDecimal scoreAwarded,
            BigDecimal maxScore,
            LiveQuizAnswerStatus answerStatus,
            Integer responseTimeMs,
            OffsetDateTime answeredAt,
            OffsetDateTime questionStartedAt,
            OffsetDateTime questionEndsAt,
            OffsetDateTime serverReceivedAt,
            QuestionSnapshot questionSnapshot
    ) {
    }

    // Snapshot cau hoi tai thoi diem close de ket qua khong doi neu question-service bi sua sau nay.
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

    // Snapshot option dung cho man hinh teacher xem lai chi tiet.
    public record OptionSnapshot(
            UUID optionId,
            String key,
            String content,
            String contentFormat
    ) {
    }
}
