package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
// Service xu ly luu nháp tu dong (autosave) cau tra loi cua hoc sinh trong ca thi
public class StudentAutosaveService {

    private final ExamSessionRepo examSessionRepo;
    private final SessionAnswerRepo sessionAnswerRepo;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StudentAutosaveService(
            ExamSessionRepo examSessionRepo,
            SessionAnswerRepo sessionAnswerRepo,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.sessionAnswerRepo = sessionAnswerRepo;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    // Tu dong luu nhap danh sach dap an da chon
    public AutosaveResponseDTO autosave(UUID sessionId, AutosaveRequestDTO request, UUID studentId) {
        // 1. Khoa phien lam bai de thuc hien ghi nhan dap an tranh xung dot ghi dong thoi
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

        // 2. Load cau truc de thi da tron de validate tinh hop le cua cau hoi va phuong an lua chon
        List<UUID> qOrder;
        Map<UUID, List<UUID>> optOrders;
        try {
            qOrder = objectMapper.readValue(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
            optOrders = objectMapper.readValue(session.getOptionOrders(), new TypeReference<Map<UUID, List<UUID>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("DESERIALIZATION_FAILED", e);
        }

        Set<UUID> questionSet = new HashSet<>(qOrder);

        // Validate thong tin request truoc khi luu
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

        // 3. Tai toan bo cau tra loi hien tai cua phien lam bai
        List<SessionAnswer> existingList = sessionAnswerRepo.findAllBySessionId(sessionId);
        Map<UUID, SessionAnswer> existingMap = existingList.stream()
                .collect(Collectors.toMap(SessionAnswer::getQuestionId, Function.identity()));

        int savedCount = 0;
        int skippedCount = 0;

        for (AutosaveAnswerDTO ans : requestedAnswers) {
            SessionAnswer existing = existingMap.get(ans.getQuestionId());
            String optionsJson;
            try {
                optionsJson = objectMapper.writeValueAsString(ans.getSelectedOptionIds() != null ? ans.getSelectedOptionIds() : List.of());
            } catch (Exception e) {
                throw new IllegalStateException("SERIALIZATION_FAILED", e);
            }

            if (existing == null) {
                // Them moi cau tra loi cho cau hoi
                SessionAnswer newAnswer = new SessionAnswer();
                newAnswer.setSessionId(sessionId);
                newAnswer.setExamId(session.getExamId());
                newAnswer.setStudentId(session.getStudentId());
                newAnswer.setQuestionId(ans.getQuestionId());
                newAnswer.setSelectedOptionIds(optionsJson);
                newAnswer.setAnswerText(ans.getAnswerText());
                newAnswer.setMarkedForReview(ans.isMarkedForReview());
                newAnswer.setClientSeq(clientSeq);
                newAnswer.setServerSeq(session.getAutosaveSeq() + 1);
                newAnswer.setAnsweredAt(now);
                newAnswer.setLastChangedAt(now);

                sessionAnswerRepo.save(newAnswer);
                savedCount++;
            } else {
                // Cap nhat cau tra loi cu neu clientSeq moi lon hon hoac bang seq cu (tranh ghi de do gui cu hon)
                if (clientSeq >= existing.getClientSeq()) {
                    existing.setSelectedOptionIds(optionsJson);
                    existing.setAnswerText(ans.getAnswerText());
                    existing.setMarkedForReview(ans.isMarkedForReview());
                    existing.setClientSeq(clientSeq);
                    existing.setLastChangedAt(now);

                    sessionAnswerRepo.save(existing);
                    savedCount++;
                } else {
                    skippedCount++;
                }
            }
        }

        // 4. Cap nhat cac truong metadata cua session
        session.setAutosaveSeq(session.getAutosaveSeq() + 1);
        session.setLastAutosaveAt(now);

        // Tinh toan lai so cau hoi da lam de show cho giao vien/he thong
        List<SessionAnswer> updatedList = sessionAnswerRepo.findAllBySessionId(sessionId);
        long answeredCount = updatedList.stream()
                .filter(ans -> {
                    boolean hasOptions = false;
                    try {
                        if (ans.getSelectedOptionIds() != null && !ans.getSelectedOptionIds().equals("[]") && !ans.getSelectedOptionIds().isBlank()) {
                            List<?> opts = objectMapper.readValue(ans.getSelectedOptionIds(), List.class);
                            hasOptions = opts != null && !opts.isEmpty();
                        }
                    } catch (Exception ignored) {}
                    boolean hasText = ans.getAnswerText() != null && !ans.getAnswerText().isBlank();
                    return hasOptions || hasText;
                })
                .count();

        session.setAnsweredCount((int) answeredCount);
        examSessionRepo.save(session);

        return AutosaveResponseDTO.builder()
                .sessionId(sessionId)
                .acceptedSeq(clientSeq)
                .serverSeq(session.getAutosaveSeq())
                .savedCount(savedCount)
                .skippedCount(skippedCount)
                .lastAutosaveAt(now)
                .build();
    }
}
