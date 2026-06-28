package com.examruntime_service.examruntime_service.service.session.autoSave;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.AnswerStoreMode;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
// Service xu ly autosave: validate bang DB, ghi nong vao Redis, DB fallback khi Redis loi.
public class StudentAutosaveService {

    private final ExamSessionRepo examSessionRepo;
    private final AnswerDraftStore answerDraftStore;
    private final SessionAnswerCheckpointWriter checkpointWriter;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StudentAutosaveService(
            ExamSessionRepo examSessionRepo,
            AnswerDraftStore answerDraftStore,
            SessionAnswerCheckpointWriter checkpointWriter,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.answerDraftStore = answerDraftStore;
        this.checkpointWriter = checkpointWriter;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public AutosaveResponseDTO autosave(UUID sessionId, AutosaveRequestDTO request, UUID studentId) {
        ExamSession session = examSessionRepo.findByIdForUpdate(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));

        if (!session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }

        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            throw new ConflictException("SESSION_NOT_IN_PROGRESS");
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        if (session.getServerDeadlineAt() != null && now.isAfter(session.getServerDeadlineAt())) {
            throw new ConflictException("EXAM_ALREADY_ENDED");
        }

        List<AutosaveAnswerDTO> requestedAnswers = request != null && request.getAnswers() != null
                ? request.getAnswers()
                : Collections.emptyList();
        long clientSeq = request != null ? request.getClientSeq() : 0;

        validateAnswers(session, requestedAnswers);

        AnswerDraftSaveResult saveResult;
        AnswerStoreMode storeMode;
        try {
            // luu len redis
            saveResult = answerDraftStore.saveBatch(session, requestedAnswers, clientSeq, now, resolveDraftTtl(session, now));
            storeMode = AnswerStoreMode.REDIS;
        } catch (Exception redisException) {
            // neu redis khong hoat dong thi luu xuong database
            saveResult = checkpointWriter.writeAutosaveFallback(session, requestedAnswers, clientSeq, now);
            storeMode = AnswerStoreMode.DB_FALLBACK;
        }

        return AutosaveResponseDTO.builder()
                .sessionId(sessionId)
                .acceptedSeq(clientSeq)
                .serverSeq(saveResult.serverSeq())
                .savedCount(saveResult.savedCount())
                .skippedCount(saveResult.skippedCount())
                .storeMode(storeMode.name())
                .lastAutosaveAt(saveResult.lastAutosaveAt())
                .build();
    }

    private void validateAnswers(ExamSession session, List<AutosaveAnswerDTO> requestedAnswers) {
        List<UUID> qOrder;
        Map<UUID, List<UUID>> optOrders;
        try {
            qOrder = objectMapper.readValue(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
            optOrders = objectMapper.readValue(session.getOptionOrders(), new TypeReference<Map<UUID, List<UUID>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("DESERIALIZATION_FAILED", e);
        }

        Set<UUID> questionSet = new HashSet<>(qOrder);
        for (AutosaveAnswerDTO ans : requestedAnswers) {
            if (!questionSet.contains(ans.getQuestionId())) {
                throw new ConflictException("INVALID_QUESTION_ID");
            }
            List<UUID> validOptions = optOrders.get(ans.getQuestionId());
            if (ans.getSelectedOptionIds() != null) {
                if (validOptions == null) {
                    throw new ConflictException("INVALID_OPTION_ID");
                }
                for (UUID optId : ans.getSelectedOptionIds()) {
                    if (!validOptions.contains(optId)) {
                        throw new ConflictException("INVALID_OPTION_ID");
                    }
                }
            }
        }
    }

    private Duration resolveDraftTtl(ExamSession session, OffsetDateTime now) {
        if (session.getServerDeadlineAt() == null) {
            return Duration.ofHours(24);
        }
        Duration ttl = Duration.between(now, session.getServerDeadlineAt().plusHours(24));
        return ttl.isNegative() || ttl.isZero() ? Duration.ofHours(24) : ttl;
    }
}
