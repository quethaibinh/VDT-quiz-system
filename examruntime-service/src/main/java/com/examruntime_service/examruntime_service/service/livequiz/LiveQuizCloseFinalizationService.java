package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.events.LiveQuizRoomClosedEvent;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizAnswer;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizAnswerRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantPaperRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiveQuizCloseFinalizationService {

    // Event nay khong di chung voi luong exam submit thong thuong.
    // Aggregate la phong live quiz vi ket qua chinh thuc duoc sinh ra theo hanh dong dong phong.
    public static final String OUTBOX_AGGREGATE_TYPE = "LiveQuizRoom";

    private final LiveQuizRoomRepo roomRepo;
    private final LiveQuizParticipantRepo participantRepo;
    private final LiveQuizParticipantPaperRepo paperRepo;
    private final LiveQuizAnswerRepo answerRepo;
    private final OutboxEventRepo outboxEventRepo;
    private final LiveQuizRoomCache roomCache;
    private final LiveQuizRealtimePublisher realtimePublisher;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LiveQuizCloseFinalizationService(
            LiveQuizRoomRepo roomRepo,
            LiveQuizParticipantRepo participantRepo,
            LiveQuizParticipantPaperRepo paperRepo,
            LiveQuizAnswerRepo answerRepo,
            OutboxEventRepo outboxEventRepo,
            LiveQuizRoomCache roomCache,
            LiveQuizRealtimePublisher realtimePublisher,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.roomRepo = roomRepo;
        this.participantRepo = participantRepo;
        this.paperRepo = paperRepo;
        this.answerRepo = answerRepo;
        this.outboxEventRepo = outboxEventRepo;
        this.roomCache = roomCache;
        this.realtimePublisher = realtimePublisher;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public LiveQuizRoom closeAndFinalize(UUID roomId) {
        // Khoa pessimistic de tranh 2 request close cung luc tao 2 bo ket qua khac nhau.
        // Transaction nay la bien gioi bat bien cua ket qua: sau khi commit, rank va diem duoc xem la chot.
        LiveQuizRoom room = roomRepo.findByIdForUpdate(roomId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_ROOM_NOT_FOUND"));
        if (room.getStatus() == LiveQuizRoomStatus.CLOSED) {
            return room;
        }
        if (room.getStatus() != LiveQuizRoomStatus.PREPARING
                && room.getStatus() != LiveQuizRoomStatus.OPEN
                && room.getStatus() != LiveQuizRoomStatus.STARTED) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_CLOSABLE");
        }

        // closedAt la moc thoi gian dung chung cho room, answer bi dong bang, outbox event va ket qua released.
        // Dung cung mot moc giup Result Service va UI khong bi lech thoi gian hien thi.
        OffsetDateTime closedAt = now();
        ExamPaperPoolDTO paperPool = requirePaperPool(room);
        ExamAnswerKeyDTO answerKey = requireAnswerKey(room);
        Map<UUID, PaperQuestionDTO> questions = paperPool.questions().stream()
                .collect(Collectors.toMap(PaperQuestionDTO::questionId, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        Map<UUID, AnswerEntryDTO> answerKeys = answerKey.answers().stream()
                .collect(Collectors.toMap(AnswerEntryDTO::questionId, Function.identity(), (left, right) -> left));

        List<LiveQuizParticipant> participants = participantRepo.findByRoomIdForUpdate(room.getId());
        Map<UUID, Integer> notReachedCounts = new LinkedHashMap<>();
        Map<UUID, List<LiveQuizAnswer>> finalAnswers = new LinkedHashMap<>();

        for (LiveQuizParticipant participant : participants) {
            // Moi hoc sinh co order rieng do shuffle, nen phai doc paper cua participant.
            // Khong duoc suy ra theo paperPool chung neu khong se sai vi tri cau hoi va rank chi tiet.
            List<UUID> questionOrder = questionOrder(participant);
            Map<UUID, LiveQuizAnswer> answersByQuestion = answerRepo
                    .findByRoomIdAndParticipantIdOrderByQuestionPositionAsc(room.getId(), participant.getId())
                    .stream()
                    .collect(Collectors.toMap(LiveQuizAnswer::getQuestionId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

            // Khi giao vien dong phong, Runtime phai dong bang tat ca cau con thieu.
            // Cau dang mo tinh TIMEOUT; cau chua tung mo tinh NOT_REACHED de giao vien nhin ro ly do mat diem.
            for (int index = 0; index < questionOrder.size(); index++) {
                UUID questionId = questionOrder.get(index);
                if (answersByQuestion.containsKey(questionId)) {
                    continue;
                }
                PaperQuestionDTO question = questions.get(questionId);
                if (question == null) {
                    throw new ConflictException("LIVE_QUIZ_CURRENT_QUESTION_NOT_FOUND");
                }
                LiveQuizAnswer missing = new LiveQuizAnswer();
                missing.setRoomId(room.getId());
                missing.setParticipantId(participant.getId());
                missing.setStudentId(participant.getStudentId());
                missing.setQuestionId(questionId);
                missing.setQuestionPosition(index + 1);
                missing.setSelectedOptionIds("[]");
                boolean activeQuestion = questionId.equals(participant.getCurrentQuestionId());
                missing.setAnswerStatus(activeQuestion ? LiveQuizAnswerStatus.TIMEOUT : LiveQuizAnswerStatus.NOT_REACHED);
                missing.setCorrect(false);
                missing.setScoreAwarded(BigDecimal.ZERO);
                missing.setMaxScore(BigDecimal.valueOf(question.score()));
                missing.setQuestionStartedAt(activeQuestion && participant.getCurrentQuestionStartedAt() != null
                        ? participant.getCurrentQuestionStartedAt()
                        : closedAt);
                missing.setQuestionEndsAt(activeQuestion && participant.getCurrentQuestionEndsAt() != null
                        ? participant.getCurrentQuestionEndsAt()
                        : closedAt);
                missing.setServerReceivedAt(closedAt);
                LiveQuizAnswer saved = answerRepo.save(missing);
                answersByQuestion.put(questionId, saved);
            }

            List<LiveQuizAnswer> orderedAnswers = questionOrder.stream()
                    .map(answersByQuestion::get)
                    .filter(answer -> answer != null)
                    .sorted(Comparator.comparingInt(LiveQuizAnswer::getQuestionPosition))
                    .toList();
            // Khong tin aggregate dang co san tren participant, vi giao vien co the close dot ngot.
            // Tinh lai tu answer rows sau khi da lap day cau thieu moi dam bao diem/rank la ban chot.
            recalculateParticipant(participant, orderedAnswers, closedAt);
            notReachedCounts.put(participant.getId(), (int) orderedAnswers.stream()
                    .filter(answer -> answer.getAnswerStatus() == LiveQuizAnswerStatus.NOT_REACHED)
                    .count());
            finalAnswers.put(participant.getId(), orderedAnswers);
        }

        // Rank chi duoc gan sau khi tat ca participant da co du bo answer chot.
        // Thu tu tie-breaker nam trong rankedParticipants de teacher/student nhin cung mot ket qua.
        List<LiveQuizParticipant> ranked = rankedParticipants(participants);
        for (int index = 0; index < ranked.size(); index++) {
            ranked.get(index).setCurrentRank(index + 1);
        }
        participantRepo.saveAll(participants);

        room.setStatus(LiveQuizRoomStatus.CLOSED);
        room.setClosedAt(closedAt);
        LiveQuizRoom savedRoom = roomRepo.save(room);
        roomCache.putRoomStatus(savedRoom, paperPool.snapshotVersion());

        // Event gom du snapshot cau hoi + dap an dung de Result Service khong can goi nguoc runtime/exam-service.
        // Day la mau "dong goi snapshot" de ket qua sau nay khong doi neu quiz/question bi sua.
        LiveQuizRoomClosedEvent event = buildEvent(savedRoom, paperPool, questions, answerKeys, ranked, finalAnswers, notReachedCounts, closedAt);
        writeOutbox(savedRoom, event);
        publishClosedRealtime(savedRoom, ranked);
        return savedRoom;
    }

    private void recalculateParticipant(LiveQuizParticipant participant, List<LiveQuizAnswer> answers, OffsetDateTime closedAt) {
        // Aggregate tren participant duoc dung cho leaderboard realtime va event ket qua.
        // Vi vay ham nay tinh lai toan bo thay vi cong don tiep de tranh sai so khi close lai/idempotent.
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int answered = 0;
        int correct = 0;
        int wrong = 0;
        int timeout = 0;
        int totalResponse = 0;
        int responseCount = 0;
        for (LiveQuizAnswer answer : answers) {
            maxScore = maxScore.add(answer.getMaxScore());
            totalScore = totalScore.add(answer.getScoreAwarded());
            if (answer.getAnswerStatus() == LiveQuizAnswerStatus.ANSWERED) {
                answered++;
                if (answer.isCorrect()) {
                    correct++;
                } else {
                    wrong++;
                }
                if (answer.getResponseTimeMs() != null) {
                    totalResponse += answer.getResponseTimeMs();
                    responseCount++;
                }
            } else if (answer.getAnswerStatus() == LiveQuizAnswerStatus.TIMEOUT) {
                answered++;
                timeout++;
            }
        }
        participant.setTotalScore(totalScore);
        participant.setMaxScore(maxScore);
        participant.setAnsweredCount(answered);
        participant.setCorrectCount(correct);
        participant.setWrongCount(wrong);
        participant.setTimeoutCount(timeout);
        participant.setAverageResponseMs(responseCount > 0 ? totalResponse / responseCount : null);
        participant.setStatus(LiveQuizParticipantStatus.FINISHED);
        participant.setCurrentQuestionId(null);
        participant.setCurrentQuestionStartedAt(null);
        participant.setCurrentQuestionEndsAt(null);
        if (participant.getFinishedAt() == null) {
            participant.setFinishedAt(closedAt);
        }
        participant.setLastSeenAt(closedAt);
    }

    private LiveQuizRoomClosedEvent buildEvent(
            LiveQuizRoom room,
            ExamPaperPoolDTO paperPool,
            Map<UUID, PaperQuestionDTO> questions,
            Map<UUID, AnswerEntryDTO> answerKeys,
            List<LiveQuizParticipant> ranked,
            Map<UUID, List<LiveQuizAnswer>> finalAnswers,
            Map<UUID, Integer> notReachedCounts,
            OffsetDateTime closedAt
    ) {
        // Sinh eventId mot lan cho outbox. Result Service dung eventId lam inbox key de xu ly idempotent.
        UUID eventId = UUID.randomUUID();
        List<LiveQuizRoomClosedEvent.ParticipantResult> participants = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            LiveQuizParticipant participant = ranked.get(index);
            List<LiveQuizRoomClosedEvent.AnswerResult> answers = finalAnswers.getOrDefault(participant.getId(), List.of())
                    .stream()
                    .map(answer -> toAnswerResult(answer, questions.get(answer.getQuestionId()), answerKeys.get(answer.getQuestionId())))
                    .toList();
            participants.add(new LiveQuizRoomClosedEvent.ParticipantResult(
                    participant.getId(),
                    participant.getStudentId(),
                    participant.getStudentCodeSnapshot(),
                    participant.getStudentNameSnapshot(),
                    index + 1,
                    participant.getStatus(),
                    participant.getAnsweredCount(),
                    participant.getCorrectCount(),
                    participant.getWrongCount(),
                    participant.getTimeoutCount(),
                    notReachedCounts.getOrDefault(participant.getId(), 0),
                    participant.getTotalScore(),
                    participant.getMaxScore(),
                    participant.getAverageResponseMs(),
                    participant.getJoinedAt(),
                    participant.getStartedAt(),
                    participant.getFinishedAt(),
                    answers
            ));
        }
        return new LiveQuizRoomClosedEvent(
                eventId,
                LiveQuizRoomClosedEvent.EVENT_TYPE,
                LiveQuizRoomClosedEvent.PRODUCER,
                closedAt,
                room.getId(),
                room.getExamId(),
                room.getOwnerTeacherId(),
                room.getRoomCode(),
                room.getQuizTitle(),
                room.getSubjectId(),
                room.getSubjectName(),
                paperPool.snapshotVersion(),
                room.getQuestionCount(),
                closedAt,
                participants
        );
    }

    private LiveQuizRoomClosedEvent.AnswerResult toAnswerResult(
            LiveQuizAnswer answer,
            PaperQuestionDTO question,
            AnswerEntryDTO answerKey
    ) {
        // Neu snapshot cau hoi hoac answer key thieu thi dung transaction.
        // Khong nen day event thieu du lieu sang Result Service vi ket qua official se khong the audit.
        if (question == null || answerKey == null) {
            throw new ConflictException("LIVE_QUIZ_FINALIZATION_SNAPSHOT_INCOMPLETE");
        }
        return new LiveQuizRoomClosedEvent.AnswerResult(
                answer.getQuestionId(),
                answer.getQuestionPosition(),
                readUuidList(answer.getSelectedOptionIds()),
                answerKey.correctOptionIds(),
                answer.isCorrect(),
                answer.getScoreAwarded(),
                answer.getMaxScore(),
                answer.getAnswerStatus(),
                answer.getResponseTimeMs(),
                answer.getAnsweredAt(),
                answer.getQuestionStartedAt(),
                answer.getQuestionEndsAt(),
                answer.getServerReceivedAt(),
                new LiveQuizRoomClosedEvent.QuestionSnapshot(
                        question.questionId(),
                        question.questionVersion(),
                        question.difficulty(),
                        question.type(),
                        question.content(),
                        question.contentFormat(),
                        question.score(),
                        question.timeLimitSeconds(),
                        question.imageObjectKey(),
                        question.options().stream()
                                .map(option -> new LiveQuizRoomClosedEvent.OptionSnapshot(
                                        option.optionId(),
                                        option.key(),
                                        option.content(),
                                        option.contentFormat()
                                ))
                                .toList()
                )
        );
    }

    private void writeOutbox(LiveQuizRoom room, LiveQuizRoomClosedEvent event) {
        // Outbox dam bao neu Kafka tam loi thi event van con trong DB de worker retry.
        // Check ton tai theo room + event type de close request lap lai khong tao them ket qua.
        if (outboxEventRepo.findFirstByAggregateTypeAndAggregateIdAndEventType(
                OUTBOX_AGGREGATE_TYPE,
                room.getId(),
                LiveQuizRoomClosedEvent.EVENT_TYPE
        ).isPresent()) {
            return;
        }
        outboxEventRepo.save(new OutboxEvent(
                event.eventId(),
                OUTBOX_AGGREGATE_TYPE,
                room.getId(),
                LiveQuizRoomClosedEvent.EVENT_TYPE,
                writeJson(event),
                writeJson(Map.of(
                        "roomId", room.getId(),
                        "examId", room.getExamId(),
                        "eventType", LiveQuizRoomClosedEvent.EVENT_TYPE
                )),
                OutboxStatus.PENDING,
                0,
                null,
                null,
                null,
                LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                null
        ));
    }

    private void publishClosedRealtime(LiveQuizRoom room, List<LiveQuizParticipant> ranked) {
        // Realtime chi bao UI chuyen trang/refresh ket qua.
        // Nguon su that cua ket qua van la event da ghi outbox va Result Service persistence.
        realtimePublisher.publish(room.getId(), new com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO(
                room.getId(),
                room.getExamId(),
                "ROOM_CLOSED",
                now(),
                room.getStatus(),
                null,
                null,
                rankedParticipantsToLeaderboard(ranked),
                null
        ));
    }

    private List<com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO> rankedParticipantsToLeaderboard(List<LiveQuizParticipant> ranked) {
        List<com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO> leaderboard = new ArrayList<>();
        for (int index = 0; index < ranked.size(); index++) {
            LiveQuizParticipant participant = ranked.get(index);
            leaderboard.add(new com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO(
                    index + 1,
                    participant.getId(),
                    participant.getStudentId(),
                    participant.getStudentNameSnapshot(),
                    participant.getTotalScore(),
                    participant.getAnsweredCount(),
                    participant.getCorrectCount(),
                    participant.getTimeoutCount(),
                    participant.getAverageResponseMs(),
                    true
            ));
        }
        return leaderboard;
    }

    private List<LiveQuizParticipant> rankedParticipants(List<LiveQuizParticipant> participants) {
        // Tie-breaker uu tien diem, tien do, so cau dung, toc do tra loi, thoi diem hoan thanh, roi thoi diem vao phong.
        // Thu tu nay can on dinh de student va teacher cung nhin mot bang xep hang sau khi chot.
        List<LiveQuizParticipant> sorted = new ArrayList<>(participants);
        sorted.sort(Comparator
                .comparing(LiveQuizParticipant::getTotalScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(LiveQuizParticipant::getAnsweredCount, Comparator.reverseOrder())
                .thenComparing(LiveQuizParticipant::getCorrectCount, Comparator.reverseOrder())
                .thenComparing(LiveQuizParticipant::getAverageResponseMs, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LiveQuizParticipant::getFinishedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(LiveQuizParticipant::getJoinedAt, Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    private ExamPaperPoolDTO requirePaperPool(LiveQuizRoom room) {
        // Live quiz da hydrate snapshot vao Redis khi prepare/open/start.
        // Neu snapshot mat thi dung close de tranh sinh ket qua khong co noi dung cau hoi.
        ExamPaperPoolDTO paperPool = roomCache.getPaperPool(room.getId());
        if (paperPool == null || paperPool.questions() == null) {
            throw new ConflictException("LIVE_QUIZ_PAPER_NOT_READY");
        }
        return paperPool;
    }

    private ExamAnswerKeyDTO requireAnswerKey(LiveQuizRoom room) {
        ExamAnswerKeyDTO answerKey = roomCache.getAnswerKey(room.getId());
        if (answerKey == null || answerKey.answers() == null) {
            throw new ConflictException("LIVE_QUIZ_ANSWER_KEY_NOT_READY");
        }
        return answerKey;
    }

    private List<UUID> questionOrder(LiveQuizParticipant participant) {
        // Paper cua participant la bang chung cu ve cac cau hoc sinh duoc giao.
        // Dung order nay de tao NOT_REACHED cho dung so cau va dung vi tri hien thi.
        LiveQuizParticipantPaper paper = paperRepo.findByParticipantId(participant.getId())
                .orElseThrow(() -> new ConflictException("LIVE_QUIZ_PARTICIPANT_PAPER_NOT_FOUND"));
        try {
            return objectMapper.readValue(paper.getQuestionOrder(), new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_QUESTION_ORDER_READ_FAILED", exception);
        }
    }

    private List<UUID> readUuidList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_FINALIZATION_SERIALIZATION_FAILED", exception);
        }
    }

    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
