package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.model.dto.session.StudentJoinResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
// Service quan ly viec tham gia (join) ca thi cua hoc sinh
public class StudentExamJoinService {

    private final RuntimeActivationResolver activationResolver;
    private final RuntimeAssignmentResolver assignmentResolver;
    private final ExamSessionRepo examSessionRepo;
    private final Clock clock;

    public StudentExamJoinService(
            RuntimeActivationResolver activationResolver,
            RuntimeAssignmentResolver assignmentResolver,
            ExamSessionRepo examSessionRepo,
            Clock clock
    ) {
        this.activationResolver = activationResolver;
        this.assignmentResolver = assignmentResolver;
        this.examSessionRepo = examSessionRepo;
        this.clock = clock;
    }

    @Transactional
    // Hoc sinh tham gia ca thi, tao phien lam bai nhung chua bat dau bam gio lam bai
    public StudentJoinResponseDTO joinExam(UUID examId, UUID studentId) {
        // 1. Kiem tra thong tin ca thi kich hoat (READY)
        RuntimeActivationMetadata metadata = activationResolver.getReadyActivation(examId);

        // 2. Kiem tra phan cong hoc sinh duoc thi ca nay hay khong
        UUID assignmentId = assignmentResolver.resolveAssignmentId(examId, studentId);
        if (assignmentId == null) {
            throw new ConflictException("STUDENT_NOT_ASSIGNED");
        }

        // 3. Kiem tra thoi gian ket thuc truoc, vi het ca thi thi khong duoc vao lai
        OffsetDateTime now = OffsetDateTime.now(clock);
        if (metadata.endAt() != null && now.isAfter(metadata.endAt())) {
            throw new ConflictException("EXAM_ALREADY_ENDED");
        }

        // 4. Neu hoc sinh da co session thi tra ve trang thai hien tai de frontend resume/start dung luong
        Optional<ExamSession> existingSession = examSessionRepo.findByExamIdAndStudentIdAndAttemptNo(examId, studentId, 1);
        ExamSession session;
        if (existingSession.isPresent()) {
            session = existingSession.get();
            // neu bai thi bi lock (sinh vien vi pham nhieu qua) thi se khong duoc vao thi nua
            if(session.getStatus().equals(ExamSessionStatus.LOCKED)){
                throw new ConflictException("EXAM_ALREADY_LOCKED");
            }
            session.setLastSeenAt(now);
            session = examSessionRepo.save(session);
        } else {
            // 5. Chi chan cua so join voi hoc sinh chua tung tao session trong ca thi
            validateJoinWindow(metadata, now);
            session = createSession(examId, studentId, assignmentId, metadata.snapshotVersion(), now);
        }

        boolean canStart = (now.isAfter(metadata.startAt()) || now.isEqual(metadata.startAt()))
                && (metadata.endAt() == null || now.isBefore(metadata.endAt()));
        long remainingSecondsToStart = Math.max(0, Duration.between(now, metadata.startAt()).toSeconds());

        return StudentJoinResponseDTO.builder()
                .sessionId(session.getId())
                .examId(examId)
                .status(session.getStatus())
                .serverTime(now)
                .startAt(metadata.startAt())
                .endAt(metadata.endAt())
                .canStart(canStart)
                .remainingSecondsToStart(remainingSecondsToStart)
                .build();
    }

    private void validateJoinWindow(RuntimeActivationMetadata metadata, OffsetDateTime now) {
        OffsetDateTime waitingRoomOpenAt = metadata.startAt().minusMinutes(metadata.joinBeforeMinutes());

        OffsetDateTime latestJoinAt = metadata.startAt().plusMinutes(metadata.joinAfterMinutes());
        if (metadata.endAt() != null && metadata.endAt().isBefore(latestJoinAt)) {
            latestJoinAt = metadata.endAt();
        }

        if (now.isBefore(waitingRoomOpenAt)) {
            throw new ConflictException("EXAM_JOIN_NOT_OPEN");
        }
        if (now.isAfter(latestJoinAt)) {
            throw new ConflictException("EXAM_LATE_JOIN_CLOSED");
        }
    }

    private ExamSession createSession(UUID examId, UUID studentId, UUID assignmentId, int snapshotVersion, OffsetDateTime now) {
        try {
            ExamSession session = new ExamSession();
            session.setExamId(examId);
            session.setStudentId(studentId);
            session.setAssignmentId(assignmentId);
            session.setSnapshotVersion(snapshotVersion);
            session.setAttemptNo(1);
            session.setStatus(ExamSessionStatus.CREATED);
            session.setLastSeenAt(now);
            session.setQuestionOrder("[]");
            session.setOptionOrders("{}");
            return examSessionRepo.saveAndFlush(session);
        } catch (DataIntegrityViolationException ex) {
            // Truong hop bi concurrency tranh chap insert, query lai record da duoc insert truoc
            return examSessionRepo.findByExamIdAndStudentIdAndAttemptNo(examId, studentId, 1)
                    .orElseThrow(() -> ex);
        }
    }
}
