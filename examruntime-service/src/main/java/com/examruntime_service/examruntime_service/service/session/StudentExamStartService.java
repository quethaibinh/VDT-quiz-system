package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.model.dto.session.StudentPaperResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionAnswer;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperGenerator;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
// Service quan ly viec bat dau lam bai (start) thi cua hoc sinh
public class StudentExamStartService {

    private final ExamSessionRepo examSessionRepo;
    private final SessionAnswerRepo sessionAnswerRepo;
    private final RuntimeActivationResolver activationResolver;
    private final RuntimePaperPoolLoader paperPoolLoader;
    private final StudentPaperGenerator paperGenerator;
    private final StudentPaperMapper paperMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StudentExamStartService(
            ExamSessionRepo examSessionRepo,
            SessionAnswerRepo sessionAnswerRepo,
            RuntimeActivationResolver activationResolver,
            RuntimePaperPoolLoader paperPoolLoader,
            StudentPaperGenerator paperGenerator,
            StudentPaperMapper paperMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.sessionAnswerRepo = sessionAnswerRepo;
        this.activationResolver = activationResolver;
        this.paperPoolLoader = paperPoolLoader;
        this.paperGenerator = paperGenerator;
        this.paperMapper = paperMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    // Bat dau phien lam bai (chuyen tu CREATED sang IN_PROGRESS) hoac tra ve de da sinh neu dang lam do
    public StudentPaperResponseDTO startExam(UUID examId, UUID studentId) {
        // 1. Khoa phien lam bai bang PESSIMISTIC_WRITE de tranh race condition
        ExamSession session = examSessionRepo.findByExamStudentAttemptForUpdate(examId, studentId, 1)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));

        // 2. Check chu so huu phien lam bai
        if (!session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }

        // 3. Kiem tra trang thai ca thi va timing window
        RuntimeActivationMetadata metadata = activationResolver.getReadyActivation(examId);
        OffsetDateTime now = OffsetDateTime.now(clock);

        if (now.isBefore(metadata.startAt())) {
            throw new ConflictException("EXAM_NOT_STARTED");
        }
        if (metadata.endAt() != null && now.isAfter(metadata.endAt())) {
            throw new ConflictException("EXAM_ALREADY_ENDED");
        }

        Duration ttl = Duration.between(now, metadata.endAt().plusHours(24));
        if (ttl.isNegative() || ttl.isZero()) {
            ttl = Duration.ofHours(24);
        }

        ExamPaperPoolDTO pool = paperPoolLoader.load(examId, metadata.snapshotVersion(), ttl);

        List<UUID> qOrder;
        Map<UUID, List<UUID>> optOrders;

        if (session.getStatus() == ExamSessionStatus.CREATED) {
            // Tien hanh sinh de ca nhan va persist vao DB
            long seed = paperGenerator.generateSeed(examId, studentId, metadata.snapshotVersion());
            StudentPaperGenerator.PaperStructure structure = paperGenerator.generatePaperStructure(pool, seed);

            try {
                session.setPaperSeed(seed);
                session.setQuestionOrder(objectMapper.writeValueAsString(structure.questionOrder));
                session.setOptionOrders(objectMapper.writeValueAsString(structure.optionOrders));
            } catch (Exception e) {
                throw new IllegalStateException("SERIALIZATION_FAILED", e);
            }

            session.setStatus(ExamSessionStatus.IN_PROGRESS);
            session.setServerStartedAt(now);
            session.setServerDeadlineAt(metadata.endAt());
            session.setLastSeenAt(now);

            examSessionRepo.save(session);

            qOrder = structure.questionOrder;
            optOrders = structure.optionOrders;
        } else if (session.getStatus() == ExamSessionStatus.IN_PROGRESS) {
            // Tra ve de thi da duoc sinh tu truoc ma khong shuffle lai
            try {
                qOrder = objectMapper.readValue(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
                optOrders = objectMapper.readValue(session.getOptionOrders(), new TypeReference<Map<UUID, List<UUID>>>() {});
            } catch (Exception e) {
                throw new IllegalStateException("DESERIALIZATION_FAILED", e);
            }
        } else {
            throw new ConflictException("SESSION_STATUS_INVALID");
        }

        // Lay tat ca cau tra loi da duoc luu neu co (khi resume lai phien dang lam)
        List<SessionAnswer> savedAnswers = sessionAnswerRepo.findAllBySessionId(session.getId());

        return StudentPaperResponseDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .serverStartedAt(session.getServerStartedAt())
                .serverDeadlineAt(session.getServerDeadlineAt())
                .questions(paperMapper.mapToStudentQuestions(pool, qOrder, optOrders))
                .answers(paperMapper.mapToStudentAnswers(savedAnswers))
                .build();
    }

}
