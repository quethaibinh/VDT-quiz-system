package com.examruntime_service.examruntime_service.service.session.submit;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.events.SubmissionCreatedEvent;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.SubmitResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.Submission;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.OutboxStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmissionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import com.examruntime_service.examruntime_service.repository.SubmissionRepo;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.service.session.resume.AnswerSnapshotReader;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
// Dong bang mot lan duy nhat cho ca submit thu cong va TIME_UP.
public class SubmissionFinalizationService {

    public static final String OUTBOX_AGGREGATE_TYPE = "Submission";

    private final ExamSessionRepo examSessionRepo;
    private final SubmissionRepo submissionRepo;
    private final OutboxEventRepo outboxEventRepo;
    private final AnswerSnapshotReader answerSnapshotReader;
    private final RuntimePaperPoolLoader paperPoolLoader;
    private final RuntimeActivationCache activationCache;
    private final ExamServiceSnapshotClient snapshotClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public SubmissionFinalizationService(
            ExamSessionRepo examSessionRepo,
            SubmissionRepo submissionRepo,
            OutboxEventRepo outboxEventRepo,
            AnswerSnapshotReader answerSnapshotReader,
            RuntimePaperPoolLoader paperPoolLoader,
            RuntimeActivationCache activationCache,
            ExamServiceSnapshotClient snapshotClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.submissionRepo = submissionRepo;
        this.outboxEventRepo = outboxEventRepo;
        this.answerSnapshotReader = answerSnapshotReader;
        this.paperPoolLoader = paperPoolLoader;
        this.activationCache = activationCache;
        this.snapshotClient = snapshotClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    // check dieu kien thoi gian co dung de nop bai hay khong
    public boolean canAcceptManualFlush(UUID sessionId, UUID studentId, OffsetDateTime now) {
        ExamSession session = examSessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));
        if (!session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }
        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            return false;
        }
        if (session.getServerDeadlineAt() != null && now.isAfter(session.getServerDeadlineAt())) {
            return false;
        }
        return true;
    }

    @Transactional
    // ham xu ly logic nop bai cua sinh vien
    public SubmitResponseDTO finalizeSubmission(
            UUID sessionId,
            UUID studentId,
            SubmitReason submitReason,
            String idempotencyKey
    ) {
        // Lock session de chi mot luong submit/auto-submit duoc tao bien nhan tai mot thoi diem.
        ExamSession session = examSessionRepo.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));

        if (studentId != null && !session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }

        // Moi session chi co mot submission; retry sau khi da nop se tra ve bien nhan cu.
        Submission existingForSession = submissionRepo.findBySessionId(sessionId).orElse(null);
        if (existingForSession != null) {
            return toResponse(existingForSession);
        }

        // Idempotency key bao ve truong hop frontend retry cung request nhung DB da commit submission.
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Submission existingForKey = submissionRepo.findByIdempotencyKey(idempotencyKey).orElse(null);
            if (existingForKey != null) {
                if (!existingForKey.getSessionId().equals(sessionId)) {
                    throw new ConflictException("IDEMPOTENCY_KEY_REUSED");
                }
                return toResponse(existingForKey);
            }
        }

        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new ConflictException("SESSION_NOT_IN_PROGRESS");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        // Submit thu cong den tre sau deadline duoc ghi nhan thanh TIME_UP de ket qua nhat quan.
        SubmitReason actualSubmitReason = submitReason == SubmitReason.STUDENT
                && session.getServerDeadlineAt() != null
                && now.isAfter(session.getServerDeadlineAt())
                ? SubmitReason.TIME_UP
                : submitReason;
        // TIME_UP lay moc submittedAt la deadline, khong phai thoi diem worker quet cham.
        OffsetDateTime submittedAt = actualSubmitReason == SubmitReason.TIME_UP
                && session.getServerDeadlineAt() != null
                ? session.getServerDeadlineAt()
                : now;

        // Lay snapshot dap an moi nhat tu Redis/DB fallback truoc khi tao event cham diem.
        List<StudentAnswerDTO> answers = answerSnapshotReader.readForResume(session, resolveDraftTtl(session, now));
        Duration draftTtl = resolveDraftTtl(session, now);

        // Paper snapshot dong bang de Result Service cham theo de hoc sinh da nhin thay luc lam bai.
        Map<String, Object> paperSnapshot = paperSnapshot(session, draftTtl);
        UUID eventId = UUID.randomUUID();

        // luu phien nop bai vao database
        Submission submission = new Submission();
        submission.setSessionId(session.getId());
        submission.setExamId(session.getExamId());
        submission.setStudentId(session.getStudentId());
        submission.setAttemptNo(session.getAttemptNo());
        submission.setStatus(SubmissionStatus.RECEIVED);
        submission.setSubmitReason(actualSubmitReason);
        submission.setSubmittedAt(submittedAt);
        submission.setReceivedAt(now);
        submission.setAnswerSnapshot(writeJson(answers));
        submission.setPaperSnapshot(writeJson(paperSnapshot));
        submission.setAnswerCount((int) answers.stream().filter(this::isAnswered).count());
        submission.setIdempotencyKey(idempotencyKey);
        submission.setMessageId(eventId);
        submission.setQueuedAt(now);
        Submission saved = submissionRepo.save(submission);

        // update lai status, ... cua ban ghi session trong examSession
        session.setStatus(actualSubmitReason == SubmitReason.TIME_UP
                ? ExamSessionStatus.AUTO_SUBMITTED
                : ExamSessionStatus.SUBMITTED);
        session.setSubmittedAt(submittedAt);
        session.setSubmitReason(actualSubmitReason);
        session.setAnsweredCount(submission.getAnswerCount());
        examSessionRepo.save(session);

        // Outbox event nam chung transaction voi Submission de khong mat event neu Kafka tam thoi loi.
        SubmissionCreatedEvent event = new SubmissionCreatedEvent(
                eventId,
                SubmissionCreatedEvent.EVENT_TYPE,
                SubmissionCreatedEvent.PRODUCER,
                now,
                saved.getId(),
                session.getId(),
                session.getExamId(),
                session.getStudentId(),
                session.getAttemptNo(),
                session.getSnapshotVersion(),
                actualSubmitReason,
                submittedAt,
                answers,
                paperSnapshot
        );
        outboxEventRepo.save(new OutboxEvent(
                eventId,
                OUTBOX_AGGREGATE_TYPE,
                saved.getId(),
                SubmissionCreatedEvent.EVENT_TYPE,
                writeJson(event),
                "{}",
                OutboxStatus.PENDING,
                0,
                null,
                null,
                null,
                LocalDateTime.now(clock),
                LocalDateTime.now(clock),
                null
        ));

        return toResponse(saved);
    }

    // Lay de, cau hoi, dap an tren Redis va snapshot vao database va gui sang Kafka.
    private Map<String, Object> paperSnapshot(ExamSession session, Duration ttl) {
        // Dung thu tu cau hoi/options da luu tren session, khong shuffle lai khi nop bai.
        List<UUID> questionOrder = readJson(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
        Map<UUID, List<UUID>> optionOrders = readJson(session.getOptionOrders(), new TypeReference<Map<UUID, List<UUID>>>() {});
        ExamPaperPoolDTO paperPool = paperPoolLoader.load(session.getExamId(), session.getSnapshotVersion(), ttl);
        ExamAnswerKeyDTO answerKey = loadAnswerKey(session, ttl);
        Map<UUID, PaperQuestionDTO> paperByQuestion = paperPool.questions().stream()
                .collect(Collectors.toMap(PaperQuestionDTO::questionId, Function.identity()));
        Map<UUID, AnswerEntryDTO> answerByQuestion = answerKey.answers().stream()
                .collect(Collectors.toMap(AnswerEntryDTO::questionId, Function.identity()));

        List<Map<String, Object>> questions = questionOrder.stream()
                .map(questionId -> questionSnapshot(
                        questionId,
                        questionOrder.indexOf(questionId) + 1,
                        paperByQuestion.get(questionId),
                        answerByQuestion.get(questionId),
                        optionOrders.getOrDefault(questionId, List.of())
                ))
                .toList();

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("questions", questions);
        snapshot.put("questionOrder", questionOrder);
        snapshot.put("optionOrders", optionOrders);
        snapshot.put("snapshotVersion", session.getSnapshotVersion());
        snapshot.put("paperSeed", session.getPaperSeed());
        snapshot.put("examSnapshot", examSnapshot(session));
        snapshot.put("studentSnapshot", Map.of("studentId", session.getStudentId()));
        return snapshot;
    }

    private Map<String, Object> examSnapshot(ExamSession session) {
        RuntimeActivationMetadata metadata = activationCache.getActivation(session.getExamId());
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("examId", session.getExamId());
        snapshot.put("snapshotVersion", session.getSnapshotVersion());
        if (metadata != null) {
            snapshot.put("code", metadata.code());
            snapshot.put("title", metadata.title());
            snapshot.put("subjectId", metadata.subjectId());
            snapshot.put("subjectName", metadata.subjectName());
            snapshot.put("ownerTeacherId", metadata.ownerTeacherId());
            snapshot.put("startAt", metadata.startAt());
            snapshot.put("endAt", metadata.endAt());
            snapshot.put("showResultPolicy", metadata.showResultPolicy());
        }
        return snapshot;
    }

    // Chuyen doi question va option tu ID sang ban day du de snapshot.
    private Map<String, Object> questionSnapshot(
            UUID questionId,
            int questionOrder,
            PaperQuestionDTO question,
            AnswerEntryDTO answerKey,
            List<UUID> optionOrder
    ) {
        if (question == null || answerKey == null) {
            throw new IllegalStateException("SUBMISSION_GRADING_SNAPSHOT_INCOMPLETE");
        }
        // Snapshot chi chua noi dung can hien thi/audit; correctOptionIds nam rieng cho cham diem.
        Map<UUID, Object> optionById = question.options().stream()
                .collect(Collectors.toMap(
                        option -> option.optionId(),
                        option -> Map.of(
                                "optionId", option.optionId(),
                                "key", Optional.ofNullable(option.key()).orElse(""),
                                "content", Optional.ofNullable(option.content()).orElse(""),
                                "contentFormat", Optional.ofNullable(option.contentFormat()).orElse("")
                        )
                ));
        List<Object> orderedOptions = optionOrder.stream()
                .map(optionById::get)
                .filter(option -> option != null)
                .toList();
        Map<String, Object> visibleQuestion = new LinkedHashMap<>();
        visibleQuestion.put("questionId", questionId);
        visibleQuestion.put("questionVersion", question.questionVersion());
        visibleQuestion.put("difficulty", question.difficulty());
        visibleQuestion.put("type", question.type());
        visibleQuestion.put("content", question.content());
        visibleQuestion.put("contentFormat", question.contentFormat());
        visibleQuestion.put("score", question.score());
        visibleQuestion.put("options", orderedOptions);

        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("questionId", questionId);
        snapshot.put("questionOrder", questionOrder);
        snapshot.put("correctOptionIds", answerKey.correctOptionIds());
        snapshot.put("maxScore", answerKey.score());
        snapshot.put("score", answerKey.score());
        snapshot.put("questionSnapshot", visibleQuestion);
        return snapshot;
    }

    private ExamAnswerKeyDTO loadAnswerKey(ExamSession session, Duration ttl) {
        String cached = activationCache.getAnswerKey(session.getExamId(), session.getSnapshotVersion());
        if (cached != null && !cached.isBlank()) {
            try {
                // Uu tien answer key trong cache activation de nop bai khong phu thuoc exam-service.
                ExamAnswerKeyDTO answerKey = objectMapper.readValue(cached, ExamAnswerKeyDTO.class);
                verifyAnswerKey(session, answerKey);
                return answerKey;
            } catch (Exception ignored) {
            }
        }
        // Fallback goi exam-service khi cache mat/loi, sau do nap lai cache cho cac lan submit tiep theo.
        ExamAnswerKeyDTO fallback = snapshotClient.getAnswerKey(session.getExamId());
        verifyAnswerKey(session, fallback);
        activationCache.putAnswerKey(session.getExamId(), session.getSnapshotVersion(), writeJson(fallback), ttl);
        return fallback;
    }

    private void verifyAnswerKey(ExamSession session, ExamAnswerKeyDTO answerKey) {
        if (answerKey == null
                || !session.getExamId().equals(answerKey.examId())
                || session.getSnapshotVersion() != answerKey.snapshotVersion()) {
            throw new IllegalStateException("ANSWER_KEY_SNAPSHOT_MISMATCH");
        }
    }

    private <T> T readJson(String json, TypeReference<T> typeReference) {
        try {
            return objectMapper.readValue(json, typeReference);
        } catch (Exception exception) {
            throw new IllegalStateException("DESERIALIZATION_FAILED", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("SERIALIZATION_FAILED", exception);
        }
    }

    private boolean isAnswered(StudentAnswerDTO answer) {
        return (answer.getSelectedOptionIds() != null && !answer.getSelectedOptionIds().isEmpty())
                || (answer.getAnswerText() != null && !answer.getAnswerText().isBlank());
    }

    private Duration resolveDraftTtl(ExamSession session, OffsetDateTime now) {
        if (session.getServerDeadlineAt() == null) {
            return Duration.ofHours(24);
        }
        Duration ttl = Duration.between(now, session.getServerDeadlineAt().plusHours(24));
        return ttl.isNegative() || ttl.isZero() ? Duration.ofHours(24) : ttl;
    }

    private SubmitResponseDTO toResponse(Submission submission) {
        return SubmitResponseDTO.builder()
                .submissionId(submission.getId())
                .sessionId(submission.getSessionId())
                .status(submission.getStatus().name())
                .submitReason(submission.getSubmitReason())
                .submittedAt(submission.getSubmittedAt())
                .build();
    }
}
