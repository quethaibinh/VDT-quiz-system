package com.result_service.result_service.service.livequiz;

import com.result_service.result_service.model.dto.events.LiveQuizRoomClosedEvent;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.InboxMessage;
import com.result_service.result_service.model.entity.ResultAnswer;
import com.result_service.result_service.model.entity.enums.ExamStatus;
import com.result_service.result_service.model.entity.enums.InboxMessageStatus;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import com.result_service.result_service.model.entity.enums.ResultType;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.InboxMessageRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class LiveQuizResultIngestionService {

    // Version nay noi ro ket qua den tu Runtime live quiz, khong phai pipeline cham bai exam thuong.
    // Neu sau nay doi cach tinh diem/rank live quiz thi tang version de audit duoc.
    public static final String GRADER_VERSION = "live-quiz-runtime-v1";

    private final InboxMessageRepo inboxMessageRepo;
    private final ExamResultRepo examResultRepo;
    private final ResultAnswerRepo resultAnswerRepo;
    private final LiveQuizResultJsonSupport jsonSupport;
    private final Clock clock;

    public LiveQuizResultIngestionService(
            InboxMessageRepo inboxMessageRepo,
            ExamResultRepo examResultRepo,
            ResultAnswerRepo resultAnswerRepo,
            LiveQuizResultJsonSupport jsonSupport,
            Clock clock
    ) {
        this.inboxMessageRepo = inboxMessageRepo;
        this.examResultRepo = examResultRepo;
        this.resultAnswerRepo = resultAnswerRepo;
        this.jsonSupport = jsonSupport;
        this.clock = clock;
    }

    @Transactional
    public IngestionOutcome handle(String rawJson) {
        // Consumer nhan raw JSON tu Kafka de hash payload va luu inbox.
        // Viec nay giup retry/idempotent ro rang hon so voi deserialize roi bo mat noi dung goc.
        LiveQuizRoomClosedEvent event = jsonSupport.fromJson(rawJson, LiveQuizRoomClosedEvent.class);
        validate(event);
        String payloadHash = jsonSupport.sha256(rawJson);
        InboxMessage existingInbox = inboxMessageRepo.findById(event.eventId()).orElse(null);
        // Neu event da xu ly va da co ket qua trong DB thi coi la duplicate.
        // Kafka co the deliver lai, nen branch nay tranh tao them ExamResult cho cung participant.
        if (existingInbox != null
                && existingInbox.getStatus() == InboxMessageStatus.PROCESSED
                && !examResultRepo.findByResultTypeAndRoomId(ResultType.LIVE_QUIZ, event.roomId()).isEmpty()) {
            return IngestionOutcome.duplicate(event.eventId(), event.roomId());
        }

        // Ghi PROCESSING truoc khi insert result de neu transaction fail thi inbox khong bi danh dau sai thanh PROCESSED.
        // saveAndFlush lam unique key message_id phat huy tac dung som trong transaction.
        InboxMessage inbox = existingInbox != null ? existingInbox : new InboxMessage();
        inbox.setMessageId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setProducer(event.producer());
        inbox.setReceivedAt(existingInbox != null ? existingInbox.getReceivedAt() : OffsetDateTime.now(clock));
        inbox.setStatus(InboxMessageStatus.PROCESSING);
        inbox.setPayloadHash(payloadHash);
        inboxMessageRepo.saveAndFlush(inbox);

        int inserted = 0;
        OffsetDateTime started = OffsetDateTime.now(clock);
        for (LiveQuizRoomClosedEvent.ParticipantResult participant : event.participants()) {
            // Dedup theo room + participant de event retry khong nhan doi ket qua cua mot hoc sinh.
            // Day la lop bao ve thu hai, sau inbox eventId.
            if (examResultRepo.findByResultTypeAndRoomIdAndParticipantId(
                    ResultType.LIVE_QUIZ,
                    event.roomId(),
                    participant.participantId()
            ).isPresent()) {
                continue;
            }
            ExamResult result = buildResult(event, participant, started);
            examResultRepo.saveAndFlush(result);
            // Answer detail duoc luu sau khi ExamResult co id de teacher xem lai tung cau.
            // Student API hien tai chi tra summary, nhung chi tiet van can cho giao vien/export/audit.
            for (LiveQuizRoomClosedEvent.AnswerResult answer : participant.answers()) {
                resultAnswerRepo.save(buildAnswer(event, result, answer));
            }
            inserted++;
        }

        inbox.setStatus(InboxMessageStatus.PROCESSED);
        inbox.setProcessedAt(OffsetDateTime.now(clock));
        inboxMessageRepo.save(inbox);
        return inserted > 0
                ? IngestionOutcome.processed(event.eventId(), event.roomId(), inserted)
                : IngestionOutcome.duplicate(event.eventId(), event.roomId());
    }

    private ExamResult buildResult(
            LiveQuizRoomClosedEvent event,
            LiveQuizRoomClosedEvent.ParticipantResult participant,
            OffsetDateTime started
    ) {
        // Reuse ExamResult entity de khong tao bang ket qua rieng, nhung phan loai bang ResultType.LIVE_QUIZ.
        // Cac field submission/session duoc map sang participant de khong cham vao logic submission exam chinh.
        OffsetDateTime gradedAt = OffsetDateTime.now(clock);
        ExamResult result = new ExamResult();
        result.setResultType(ResultType.LIVE_QUIZ);
        result.setExamId(event.examId());
        result.setStudentId(participant.studentId());
        result.setSessionId(participant.participantId());
        result.setRoomId(event.roomId());
        result.setParticipantId(participant.participantId());
        // Tao submissionId on dinh vi ExamResult co unique submission_id tu luong exam cu.
        // UUID nay chi la khoa ky thuat cho live quiz result, khong co SubmissionCreated tu Runtime.
        result.setSubmissionId(deterministicSubmissionId(event.roomId(), participant.participantId()));
        result.setAttemptNo(1);
        result.setStatus(ExamStatus.GRADED);
        result.setTotalQuestions(event.questionCount());
        result.setAnsweredQuestions(participant.answeredCount());
        result.setCorrectCount(participant.correctCount());
        result.setWrongCount(participant.wrongCount());
        result.setBlankCount(participant.timeoutCount() + participant.notReachedCount());
        result.setTotalScore(scale4(participant.totalScore()));
        result.setMaxScore(scale4(participant.maxScore()));
        result.setPercentage(percentage(participant.totalScore(), participant.maxScore()));
        result.setSubmittedAt(event.closedAt());
        result.setGradedAt(gradedAt);
        result.setGradingDurationMs(Math.toIntExact(Duration.between(started, gradedAt).toMillis()));
        result.setAverageResponseMs(participant.averageResponseMs());
        result.setFinishedAt(participant.finishedAt());
        result.setGraderVersion(GRADER_VERSION);
        // Yeu cau nghiep vu: giao vien dong quiz la release ngay, khong can review/manual publish.
        result.setReviewStatus(ResultReviewStatus.RELEASED);
        result.setReleasedAt(event.closedAt());
        result.setReleasedBy(event.ownerTeacherId());
        result.setStudentSnapshot(jsonSupport.toJson(Map.of(
                "studentId", participant.studentId(),
                "studentCode", value(participant.studentCodeSnapshot()),
                "studentName", value(participant.studentNameSnapshot())
        )));
        result.setExamSnapshot(jsonSupport.toJson(examSnapshot(event, participant)));
        return result;
    }

    private ResultAnswer buildAnswer(
            LiveQuizRoomClosedEvent event,
            ExamResult result,
            LiveQuizRoomClosedEvent.AnswerResult answer
    ) {
        // ResultAnswer giu ca selected/correct option ids va questionSnapshot de giao vien xem chi tiet.
        // Du lieu nay den tu event da dong bang, khong doc lai question-service de tranh thay doi sau close.
        ResultAnswer entity = new ResultAnswer();
        entity.setResult(result);
        entity.setExamId(event.examId());
        entity.setQuestionId(answer.questionId());
        entity.setQuestionOrder(answer.questionPosition());
        entity.setQuestionPosition(answer.questionPosition());
        entity.setSelectedOptionIds(jsonSupport.toJson(answer.selectedOptionIds() != null ? answer.selectedOptionIds() : java.util.List.of()));
        entity.setCorrectOptionIds(jsonSupport.toJson(answer.correctOptionIds() != null ? answer.correctOptionIds() : java.util.List.of()));
        entity.setCorrect(answer.correct());
        entity.setScoreAwarded(scale3(answer.scoreAwarded()));
        entity.setMaxScore(scale3(answer.maxScore()));
        entity.setGradingNote(answer.answerStatus());
        entity.setResponseTimeMs(answer.responseTimeMs());
        entity.setAnsweredAt(answer.answeredAt());
        entity.setAnswerStatus(answer.answerStatus());
        entity.setQuestionSnapshot(jsonSupport.toJson(answer.questionSnapshot()));
        return entity;
    }

    private Map<String, Object> examSnapshot(
            LiveQuizRoomClosedEvent event,
            LiveQuizRoomClosedEvent.ParticipantResult participant
    ) {
        // Snapshot phong live quiz duoc nhung trong ExamResult de query nhanh va giu lich su.
        // finalRank/participantCount nam o day vi day la ket qua chot tai thoi diem close.
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("examId", event.examId());
        snapshot.put("roomId", event.roomId());
        snapshot.put("roomCode", event.roomCode());
        snapshot.put("snapshotVersion", event.snapshotVersion());
        snapshot.put("title", event.quizTitle());
        snapshot.put("quizTitle", event.quizTitle());
        snapshot.put("subjectId", event.subjectId());
        snapshot.put("subjectName", event.subjectName());
        snapshot.put("ownerTeacherId", event.ownerTeacherId());
        snapshot.put("closedAt", event.closedAt());
        snapshot.put("releasedAt", event.closedAt());
        snapshot.put("finalRank", participant.finalRank());
        snapshot.put("participantCount", event.participants().size());
        snapshot.put("timeoutCount", participant.timeoutCount());
        snapshot.put("notReachedCount", participant.notReachedCount());
        snapshot.put("averageResponseMs", participant.averageResponseMs());
        return snapshot;
    }

    private void validate(LiveQuizRoomClosedEvent event) {
        // Chi chap nhan event dung loai LiveQuizRoomClosed.
        // Neu Kafka topic bi cau hinh nham voi event exam thuong thi fail som de khong pha du lieu.
        if (event.eventId() == null || !LiveQuizRoomClosedEvent.EVENT_TYPE.equals(event.eventType())) {
            throw new IllegalArgumentException("UNSUPPORTED_LIVE_QUIZ_RESULT_EVENT");
        }
        if (event.roomId() == null || event.examId() == null || event.participants() == null) {
            throw new IllegalArgumentException("INVALID_LIVE_QUIZ_RESULT_EVENT");
        }
    }

    private UUID deterministicSubmissionId(UUID roomId, UUID participantId) {
        // Deterministic UUID giup retry cung room/participant sinh cung khoa ky thuat.
        // Nhờ vậy unique constraint van bao ve du lieu neu co race condition hiem.
        String source = "livequiz-result:" + roomId + ":" + participantId;
        return UUID.nameUUIDFromBytes(source.getBytes(StandardCharsets.UTF_8));
    }

    private BigDecimal percentage(BigDecimal score, BigDecimal maxScore) {
        if (maxScore == null || maxScore.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        }
        return score.multiply(BigDecimal.valueOf(100)).divide(maxScore, 3, RoundingMode.HALF_UP);
    }

    private BigDecimal scale4(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal scale3(BigDecimal value) {
        return (value != null ? value : BigDecimal.ZERO).setScale(3, RoundingMode.HALF_UP);
    }

    private String value(String value) {
        return value != null ? value : "";
    }

    public record IngestionOutcome(boolean processed, UUID eventId, UUID roomId, int insertedResults) {
        static IngestionOutcome processed(UUID eventId, UUID roomId, int insertedResults) {
            return new IngestionOutcome(true, eventId, roomId, insertedResults);
        }

        static IngestionOutcome duplicate(UUID eventId, UUID roomId) {
            return new IngestionOutcome(false, eventId, roomId, 0);
        }
    }
}
