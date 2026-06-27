package com.examruntime_service.examruntime_service.service.session.resume;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentPaperResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
// Service phuc hoi de thi va dap an, uu tien Redis draft truoc DB checkpoint.
public class StudentSessionResumeService {

    private final ExamSessionRepo examSessionRepo;
    private final AnswerSnapshotReader answerSnapshotReader;
    private final RuntimePaperPoolLoader paperPoolLoader;
    private final StudentPaperMapper paperMapper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public StudentSessionResumeService(
            ExamSessionRepo examSessionRepo,
            AnswerSnapshotReader answerSnapshotReader,
            RuntimePaperPoolLoader paperPoolLoader,
            StudentPaperMapper paperMapper,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examSessionRepo = examSessionRepo;
        this.answerSnapshotReader = answerSnapshotReader;
        this.paperPoolLoader = paperPoolLoader;
        this.paperMapper = paperMapper;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public StudentPaperResponseDTO resumeSession(UUID sessionId, UUID studentId) {
        ExamSession session = examSessionRepo.findById(sessionId)
                .orElseThrow(() -> new NotFoundException("SESSION_NOT_FOUND"));

        if (!session.getStudentId().equals(studentId)) {
            throw new UnauthorizedException("UNAUTHORIZED_SESSION");
        }

        if (session.getStatus() == ExamSessionStatus.CREATED) {
            return StudentPaperResponseDTO.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .questions(Collections.emptyList())
                    .answers(Collections.emptyList())
                    .build();
        }

        if (session.getStatus() != ExamSessionStatus.IN_PROGRESS) {
            return StudentPaperResponseDTO.builder()
                    .sessionId(session.getId())
                    .status(session.getStatus())
                    .serverStartedAt(session.getServerStartedAt())
                    .serverDeadlineAt(session.getServerDeadlineAt())
                    .questions(Collections.emptyList())
                    .answers(Collections.emptyList())
                    .build();
        }

        // lay ra bo cau hoi tu redis
        OffsetDateTime now = OffsetDateTime.now(clock);
        Duration ttl = resolveResumeTtl(session, now);
        ExamPaperPoolDTO pool = paperPoolLoader.load(session.getExamId(), session.getSnapshotVersion(), ttl);

        // lay chi tiet de cua sinh vien tuong ung voi nhung cau hoi nao
        List<UUID> qOrder;
        Map<UUID, List<UUID>> optOrders;
        try {
            qOrder = objectMapper.readValue(session.getQuestionOrder(), new TypeReference<List<UUID>>() {});
            optOrders = objectMapper.readValue(session.getOptionOrders(), new TypeReference<Map<UUID, List<UUID>>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("DESERIALIZATION_FAILED", e);
        }

        return StudentPaperResponseDTO.builder()
                .sessionId(session.getId())
                .status(session.getStatus())
                .serverStartedAt(session.getServerStartedAt())
                .serverDeadlineAt(session.getServerDeadlineAt())
                .questions(paperMapper.mapToStudentQuestions(pool, qOrder, optOrders)) // lay ra de cu sinh vien
                .answers(answerSnapshotReader.readForResume(session, ttl)) // lay ra nhung dap an ma sinh vien da chon
                .build();
    }

    private Duration resolveResumeTtl(ExamSession session, OffsetDateTime now) {
        if (session.getServerDeadlineAt() == null) {
            return Duration.ofHours(24);
        }
        Duration ttl = Duration.between(now, session.getServerDeadlineAt().plusHours(24));
        return ttl.isNegative() || ttl.isZero() ? Duration.ofHours(24) : ttl;
    }
}
