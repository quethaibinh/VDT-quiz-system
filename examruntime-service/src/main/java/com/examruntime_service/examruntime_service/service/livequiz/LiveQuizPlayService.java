package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizAnswerRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizAnswerResponseDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizCurrentQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizStateDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizAnswer;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizAnswerRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantPaperRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class LiveQuizPlayService {

    private final LiveQuizRoomRepo roomRepo;
    private final LiveQuizParticipantRepo participantRepo;
    private final LiveQuizParticipantPaperRepo paperRepo;
    private final LiveQuizAnswerRepo answerRepo;
    private final LiveQuizRoomCache roomCache;
    private final LiveQuizRealtimePublisher realtimePublisher;
    private final LiveQuizTeacherSnapshotService snapshotService;
    private final LiveQuizScoringPolicy scoringPolicy;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public LiveQuizPlayService(
            LiveQuizRoomRepo roomRepo,
            LiveQuizParticipantRepo participantRepo,
            LiveQuizParticipantPaperRepo paperRepo,
            LiveQuizAnswerRepo answerRepo,
            LiveQuizRoomCache roomCache,
            LiveQuizRealtimePublisher realtimePublisher,
            LiveQuizTeacherSnapshotService snapshotService,
            LiveQuizScoringPolicy scoringPolicy,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.roomRepo = roomRepo;
        this.participantRepo = participantRepo;
        this.paperRepo = paperRepo;
        this.answerRepo = answerRepo;
        this.roomCache = roomCache;
        this.realtimePublisher = realtimePublisher;
        this.snapshotService = snapshotService;
        this.scoringPolicy = scoringPolicy;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Lay state hien tai cua student va lazy timeout neu cau dang lam da het gio.
     */
    @Transactional
    public StudentLiveQuizStateDTO state(UUID roomId, UUID studentId) {
        LiveQuizRoom room = requireRoom(roomId);
        LiveQuizParticipant participant = requireParticipantForUpdate(roomId, studentId);
        if (room.getStatus() == LiveQuizRoomStatus.STARTED) {
            // Timeout duoc tinh lazy khi student poll state/current/answer, khong can scheduler rieng.
            timeoutIfExpired(room, participant);
        }
        participant.setLastSeenAt(now());
        participantRepo.save(participant);
        return toState(room, participant);
    }

    /**
     * Lay cau hoi hien tai; neu chua co thi start cau tiep theo cho participant.
     */
    @Transactional
    public LiveQuizCurrentQuestionDTO currentQuestion(UUID roomId, UUID studentId) {
        LiveQuizRoom room = requireRoom(roomId);
        requireStarted(room);
        // tim phien tham gia choi cua student nay trong phong nay
        LiveQuizParticipant participant = requireParticipantForUpdate(roomId, studentId);
        timeoutIfExpired(room, participant); // xu ly timeout cua cau hien tai neu da het gio
        if (participant.getStatus() == LiveQuizParticipantStatus.FINISHED) {
            throw new ConflictException("LIVE_QUIZ_PARTICIPANT_FINISHED");
        }
        boolean startedNewQuestion = participant.getCurrentQuestionId() == null;
        if (startedNewQuestion) {
            // Cau hoi chi bat dau khi student lay current-question sau khi room STARTED.
            startCurrentQuestion(room, participant);
        }
        participant.setLastSeenAt(now());
        LiveQuizParticipant saved = participantRepo.save(participant);
        PaperQuestionDTO question = questionMap(room).get(saved.getCurrentQuestionId());
        if (question == null) {
            throw new ConflictException("LIVE_QUIZ_CURRENT_QUESTION_NOT_FOUND");
        }
        if (startedNewQuestion) {
            // Chi phat QUESTION_STARTED khi tao timer moi, tranh spam khi student refresh.
            publishProgress(room, saved, "QUESTION_STARTED", saved.getStudentId(), false);
        }
        return new LiveQuizCurrentQuestionDTO(
                saved.getId(),
                question.questionId(),
                saved.getCurrentQuestionPosition() + 1,
                saved.getTotalQuestions(),
                saved.getAnsweredCount(),
                saved.getTotalScore(),
                saved.getMaxScore(),
                snapshotService.currentRank(room.getId(), saved.getId()),
                question.type(),
                question.content(),
                question.contentFormat(),
                question.options(),
                saved.getCurrentQuestionStartedAt(),
                saved.getCurrentQuestionEndsAt(),
                now()
        );
    }

    /**
     * Cham dap an cua student cho cau hoi dang active va advance sang cau tiep theo.
     */
    @Transactional
    public LiveQuizAnswerResponseDTO answer(UUID roomId, UUID studentId, LiveQuizAnswerRequestDTO request) {
        LiveQuizRoom room = requireRoom(roomId);
        requireStarted(room);
        LiveQuizParticipant participant = requireParticipantForUpdate(roomId, studentId);
        if (participant.getStatus() == LiveQuizParticipantStatus.FINISHED) {
            throw new ConflictException("LIVE_QUIZ_PARTICIPANT_FINISHED");
        }
        if (participant.getCurrentQuestionId() == null) {
            throw new ConflictException("LIVE_QUIZ_NO_ACTIVE_QUESTION");
        }
        if (!participant.getCurrentQuestionId().equals(request.questionId())) {
            throw new ConflictException("LIVE_QUIZ_QUESTION_NOT_CURRENT");
        }
        if (isExpired(participant)) {
            // Neu dap an den sau deadline, ghi TIMEOUT thay vi cham dap an muon.
            LiveQuizAnswer timeout = recordTimeout(room, participant);
            advanceOrFinish(room, participant);
            LiveQuizParticipant saved = participantRepo.save(participant);
            publishProgress(room, saved, "QUESTION_TIMEOUT", saved.getStudentId(), true);
            return toAnswerResponse(timeout, participant);
        }
        LiveQuizAnswer existing = answerRepo.findByRoomIdAndParticipantIdAndQuestionId(
                roomId,
                participant.getId(),
                request.questionId()
        ).orElse(null);
        if (existing != null) {
            // Idempotent submit: neu HTTP retry cung cau hoi thi tra lai ket qua da ghi.
            return toAnswerResponse(existing, participant);
        }
        LiveQuizAnswer answer = buildAnswer(room, participant, request);
        try {
            answer = answerRepo.save(answer);
        } catch (DataIntegrityViolationException exception) {
            answer = answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participant.getId(), request.questionId())
                    .orElseThrow(() -> exception);
        }
        applyAnswerToParticipant(participant, answer);
        advanceOrFinish(room, participant);
        LiveQuizParticipant saved = participantRepo.save(participant);
        publishProgress(room, saved, "ANSWER_SUBMITTED", null, true);
        if (saved.getStatus() == LiveQuizParticipantStatus.FINISHED) {
            publishProgress(room, saved, "PARTICIPANT_FINISHED", saved.getStudentId(), true);
        }
        return toAnswerResponse(answer, saved);
    }

    /**
     * Gan cau hoi hien tai, mo timer server-side va chuyen participant sang IN_PROGRESS.
     */
    private void startCurrentQuestion(LiveQuizRoom room, LiveQuizParticipant participant) {
        List<UUID> order = questionOrder(participant);
        if (participant.getCurrentQuestionPosition() >= order.size()) {
            finishParticipant(participant);
            return;
        }
        UUID questionId = order.get(participant.getCurrentQuestionPosition());
        PaperQuestionDTO question = questionMap(room).get(questionId);
        if (question == null || question.timeLimitSeconds() == null || question.timeLimitSeconds() <= 0) {
            throw new ConflictException("LIVE_QUIZ_INVALID_CURRENT_QUESTION");
        }
        OffsetDateTime startedAt = now();
        participant.setStatus(LiveQuizParticipantStatus.IN_PROGRESS);
        if (participant.getStartedAt() == null) {
            participant.setStartedAt(startedAt);
        }
        participant.setCurrentQuestionId(questionId);
        participant.setCurrentQuestionStartedAt(startedAt);
        participant.setCurrentQuestionEndsAt(startedAt.plusSeconds(question.timeLimitSeconds()));
    }

    /**
     * Neu cau hien tai het gio thi ghi timeout va dua participant ra khoi cau do.
     */
    private void timeoutIfExpired(LiveQuizRoom room, LiveQuizParticipant participant) {
        while (participant.getStatus() == LiveQuizParticipantStatus.IN_PROGRESS
                && participant.getCurrentQuestionId() != null
                && isExpired(participant)) {
            // Moi vong chi xu ly cau hien tai; cau tiep theo chi start khi client request lai.
            recordTimeout(room, participant);
            publishProgress(room, participant, "QUESTION_TIMEOUT", participant.getStudentId(), true);
            advanceOrFinish(room, participant);
            if (participant.getStatus() != LiveQuizParticipantStatus.FINISHED) {
                break;
            }
        }
    }

    /**
     * Tao answer TIMEOUT cho cau hien tai, idempotent theo unique room/participant/question.
     */
    private LiveQuizAnswer recordTimeout(LiveQuizRoom room, LiveQuizParticipant participant) {
        LiveQuizAnswer existing = answerRepo.findByRoomIdAndParticipantIdAndQuestionId(
                room.getId(),
                participant.getId(),
                participant.getCurrentQuestionId()
        ).orElse(null);
        if (existing != null) {
            return existing;
        }
        PaperQuestionDTO question = questionMap(room).get(participant.getCurrentQuestionId());
        LiveQuizAnswer answer = baseAnswer(room, participant, question);
        answer.setAnswerStatus(LiveQuizAnswerStatus.TIMEOUT);
        answer.setCorrect(false);
        answer.setScoreAwarded(BigDecimal.ZERO);
        answer.setResponseTimeMs(null);
        try {
            LiveQuizAnswer saved = answerRepo.save(answer);
            applyAnswerToParticipant(participant, saved);
            return saved;
        } catch (DataIntegrityViolationException exception) {
            // Bao ve race submit/timeout bang unique constraint cua answer.
            return answerRepo.findByRoomIdAndParticipantIdAndQuestionId(
                    room.getId(),
                    participant.getId(),
                    participant.getCurrentQuestionId()
            ).orElseThrow(() -> exception);
        }
    }

    /**
     * Tao answer ANSWERED tu request va tinh dung/sai, diem, thoi gian phan hoi.
     */
    private LiveQuizAnswer buildAnswer(LiveQuizRoom room, LiveQuizParticipant participant, LiveQuizAnswerRequestDTO request) {
        PaperQuestionDTO question = questionMap(room).get(participant.getCurrentQuestionId());
        OffsetDateTime receivedAt = now();
        LiveQuizAnswer answer = baseAnswer(room, participant, question, receivedAt);
        List<UUID> selectedIds = request.selectedOptionIds() != null ? request.selectedOptionIds() : List.of();
        boolean correct = isCorrect(room, request.questionId(), selectedIds);
        LiveQuizScoreResult score = scoringPolicy.score(
                BigDecimal.valueOf(question.score()),
                participant.getCurrentQuestionStartedAt(),
                participant.getCurrentQuestionEndsAt(),
                receivedAt,
                correct,
                LiveQuizAnswerStatus.ANSWERED
        );
        answer.setSelectedOptionIds(writeJson(selectedIds));
        answer.setAnswerStatus(LiveQuizAnswerStatus.ANSWERED);
        answer.setAnsweredAt(receivedAt);
        answer.setResponseTimeMs(score.responseTimeMs());
        answer.setCorrect(correct);
        answer.setScoreAwarded(score.scoreAwarded());
        return answer;
    }

    /**
     * Tao cac truong chung cua LiveQuizAnswer truoc khi biet ANSWERED hay TIMEOUT.
     */
    private LiveQuizAnswer baseAnswer(LiveQuizRoom room, LiveQuizParticipant participant, PaperQuestionDTO question) {
        return baseAnswer(room, participant, question, now());
    }

    private LiveQuizAnswer baseAnswer(LiveQuizRoom room, LiveQuizParticipant participant, PaperQuestionDTO question, OffsetDateTime serverReceivedAt) {
        if (question == null) {
            throw new ConflictException("LIVE_QUIZ_CURRENT_QUESTION_NOT_FOUND");
        }
        LiveQuizAnswer answer = new LiveQuizAnswer();
        answer.setRoomId(room.getId());
        answer.setParticipantId(participant.getId());
        answer.setStudentId(participant.getStudentId());
        answer.setQuestionId(participant.getCurrentQuestionId());
        answer.setQuestionPosition(participant.getCurrentQuestionPosition() + 1);
        answer.setQuestionStartedAt(participant.getCurrentQuestionStartedAt());
        answer.setQuestionEndsAt(participant.getCurrentQuestionEndsAt());
        answer.setMaxScore(BigDecimal.valueOf(question.score()));
        answer.setServerReceivedAt(serverReceivedAt);
        return answer;
    }

    /**
     * Cong ket qua answer vao aggregate participant: count, score, correct/wrong/timeout.
     */
    private void applyAnswerToParticipant(LiveQuizParticipant participant, LiveQuizAnswer answer) {
        participant.setAnsweredCount(participant.getAnsweredCount() + 1);
        participant.setTotalScore(participant.getTotalScore().add(answer.getScoreAwarded()));
        if (answer.getAnswerStatus() == LiveQuizAnswerStatus.TIMEOUT) {
            participant.setTimeoutCount(participant.getTimeoutCount() + 1);
        } else if (answer.isCorrect()) {
            participant.setCorrectCount(participant.getCorrectCount() + 1);
        } else {
            participant.setWrongCount(participant.getWrongCount() + 1);
        }
        if (answer.getResponseTimeMs() != null) {
            // Average chi tinh tren cac cau co dap an thuc, khong tinh timeout.
            int answeredWithResponse = Math.max(1, participant.getAnsweredCount() - participant.getTimeoutCount());
            int currentAverage = participant.getAverageResponseMs() != null ? participant.getAverageResponseMs() : 0;
            participant.setAverageResponseMs(((currentAverage * (answeredWithResponse - 1)) + answer.getResponseTimeMs()) / answeredWithResponse);
        }
    }

    /**
     * Xoa current question va chuyen con tro sang cau tiep theo hoac finish participant.
     */
    private void advanceOrFinish(LiveQuizRoom room, LiveQuizParticipant participant) {
        participant.setCurrentQuestionPosition(participant.getCurrentQuestionPosition() + 1);
        participant.setCurrentQuestionId(null);
        participant.setCurrentQuestionStartedAt(null);
        participant.setCurrentQuestionEndsAt(null);
        if (participant.getCurrentQuestionPosition() >= questionOrder(participant).size()) {
            finishParticipant(participant);
        }
    }

    /**
     * Danh dau participant da hoan thanh toan bo live quiz.
     */
    private void finishParticipant(LiveQuizParticipant participant) {
        participant.setStatus(LiveQuizParticipantStatus.FINISHED);
        participant.setFinishedAt(now());
    }

    /**
     * Map answer vua ghi sang response cho student, khong tra answer key.
     */
    private LiveQuizAnswerResponseDTO toAnswerResponse(LiveQuizAnswer answer, LiveQuizParticipant participant) {
        return new LiveQuizAnswerResponseDTO(
                answer.getQuestionId(),
                answer.getQuestionPosition(),
                answer.getAnswerStatus(),
                answer.isCorrect(),
                answer.getScoreAwarded(),
                answer.getMaxScore(),
                answer.getResponseTimeMs(),
                answer.getMaxScore().compareTo(BigDecimal.ZERO) > 0
                        ? answer.getScoreAwarded().divide(answer.getMaxScore(), 4, java.math.RoundingMode.HALF_UP)
                        : BigDecimal.ZERO,
                participant.getTotalScore(),
                participant.getStatus() != LiveQuizParticipantStatus.FINISHED,
                participant.getStatus() == LiveQuizParticipantStatus.FINISHED
        );
    }

    /**
     * Map room va participant sang state nhe cho student polling/reconnect.
     */
    private StudentLiveQuizStateDTO toState(LiveQuizRoom room, LiveQuizParticipant participant) {
        return new StudentLiveQuizStateDTO(
                room.getId(),
                room.getExamId(),
                participant.getId(),
                room.getRoomCode(),
                room.getQuizTitle(),
                room.getSubjectName(),
                room.getStatus(),
                participant.getStatus(),
                participant.getAnsweredCount(),
                participant.getTotalQuestions(),
                participant.getTotalScore(),
                participant.getMaxScore(),
                snapshotService.currentRank(room.getId(), participant.getId()),
                participantRepo.findByRoomId(room.getId()).size(),
                now(),
                participant.getCurrentQuestionEndsAt()
        );
    }

    /**
     * Phat event progress len Redis de subscriber fan-out sang STOMP topic/queue.
     */
    private void publishProgress(
            LiveQuizRoom room,
            LiveQuizParticipant participant,
            String type,
            UUID studentTargetId,
            boolean includeLeaderboard
    ) {
        List<LiveQuizParticipant> participants = participantRepo.findByRoomId(room.getId());
        realtimePublisher.publish(room.getId(), new LiveQuizRealtimeMessageDTO(
                room.getId(),
                room.getExamId(),
                type,
                now(),
                room.getStatus(),
                studentTargetId,
                snapshotService.toParticipantSnapshot(participant, snapshotService.currentRank(room.getId(), participant.getId())),
                includeLeaderboard ? snapshotService.leaderboard(participants) : null,
                // Summary nhe hon leaderboard, nen gui kem moi progress event cho teacher UI.
                snapshotService.summary(participants)
        ));
    }

    /**
     * So sanh selected option voi answer key trong Redis de cham dung/sai.
     */
    private boolean isCorrect(LiveQuizRoom room, UUID questionId, List<UUID> selectedIds) {
        ExamAnswerKeyDTO answerKey = roomCache.getAnswerKey(room.getId());
        if (answerKey == null || answerKey.answers() == null) {
            throw new ConflictException("LIVE_QUIZ_ANSWER_KEY_NOT_READY");
        }
        AnswerEntryDTO entry = answerKey.answers().stream()
                .filter(answer -> answer.questionId().equals(questionId))
                .findFirst()
                .orElseThrow(() -> new ConflictException("LIVE_QUIZ_ANSWER_KEY_NOT_FOUND"));
        return new HashSet<>(entry.correctOptionIds()).equals(new HashSet<>(selectedIds));
    }

    /**
     * Tao map questionId -> PaperQuestionDTO tu paper pool trong Redis.
     */
    private Map<UUID, PaperQuestionDTO> questionMap(LiveQuizRoom room) {
        ExamPaperPoolDTO paperPool = roomCache.getPaperPool(room.getId());
        if (paperPool == null || paperPool.questions() == null) {
            throw new ConflictException("LIVE_QUIZ_PAPER_NOT_READY");
        }
        return paperPool.questions().stream()
                .collect(Collectors.toMap(PaperQuestionDTO::questionId, Function.identity()));
    }

    /**
     * Doc thu tu cau hoi ca nhan cua participant tu bang participant paper.
     */
    private List<UUID> questionOrder(LiveQuizParticipant participant) {
        LiveQuizParticipantPaper paper = paperRepo.findByParticipantId(participant.getId())
                .orElseThrow(() -> new ConflictException("LIVE_QUIZ_PARTICIPANT_PAPER_NOT_FOUND"));
        try {
            return objectMapper.readValue(paper.getQuestionOrder(), new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_QUESTION_ORDER_READ_FAILED", exception);
        }
    }

    /**
     * Serialize selectedOptionIds de luu vao cot jsonb.
     */
    private String writeJson(List<UUID> selectedIds) {
        try {
            return objectMapper.writeValueAsString(selectedIds);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_SELECTED_OPTIONS_WRITE_FAILED", exception);
        }
    }

    /**
     * Kiem tra timer cau hien tai da qua deadline server hay chua.
     */
    private boolean isExpired(LiveQuizParticipant participant) {
        return participant.getCurrentQuestionEndsAt() != null && now().isAfter(participant.getCurrentQuestionEndsAt());
    }

    /**
     * Chan cac API play neu teacher chua start room.
     */
    private void requireStarted(LiveQuizRoom room) {
        if (room.getStatus() != LiveQuizRoomStatus.STARTED) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_STARTED");
        }
    }

    /**
     * Lay room hoac nem loi not found.
     */
    private LiveQuizRoom requireRoom(UUID roomId) {
        return roomRepo.findById(roomId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_ROOM_NOT_FOUND"));
    }

    /**
     * Lay participant voi row lock de state/current-question/answer khong ghi de nhau
     * khi student vua vao man thi va client goi nhieu request gan dong thoi.
     */
    private LiveQuizParticipant requireParticipantForUpdate(UUID roomId, UUID studentId) {
        return participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_PARTICIPANT_NOT_FOUND"));
    }

    /**
     * Lay thoi gian hien tai qua Clock de test co the fake time.
     */
    private OffsetDateTime now() {
        return OffsetDateTime.now(clock);
    }
}
