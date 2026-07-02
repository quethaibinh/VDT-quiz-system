package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizJoinRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizJoinResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantPaperRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class LiveQuizStudentJoinService {

    private final LiveQuizRoomRepo roomRepo;
    private final LiveQuizParticipantRepo participantRepo;
    private final LiveQuizParticipantPaperRepo paperRepo;
    private final LiveQuizRoomCache roomCache;
    private final LiveQuizRoomService roomService;
    private final LiveQuizRealtimePublisher realtimePublisher;
    private final LiveQuizTeacherSnapshotService snapshotService;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    /**
     * Inject cac dependency can cho luong student join va tao paper ca nhan.
     */
    public LiveQuizStudentJoinService(
            LiveQuizRoomRepo roomRepo,
            LiveQuizParticipantRepo participantRepo,
            LiveQuizParticipantPaperRepo paperRepo,
            LiveQuizRoomCache roomCache,
            LiveQuizRoomService roomService,
            LiveQuizRealtimePublisher realtimePublisher,
            LiveQuizTeacherSnapshotService snapshotService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.roomRepo = roomRepo;
        this.participantRepo = participantRepo;
        this.paperRepo = paperRepo;
        this.roomCache = roomCache;
        this.roomService = roomService;
        this.realtimePublisher = realtimePublisher;
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Xu ly student join bang room code, tao participant/paper neu la lan dau.
     */
    @Transactional
    public StudentLiveQuizJoinResponseDTO join(
            UUID studentId,
            String username,
            StudentLiveQuizJoinRequestDTO request
    ) {
        String code = normalizeCode(request.code());
        LiveQuizRoom room = resolveRoom(code);
        LiveQuizParticipant existing = participantRepo.findByRoomIdAndStudentId(room.getId(), studentId).orElse(null);
        // Student moi chi duoc vao khi room dang OPEN. Sau STARTED la khoa join muon.
        if (existing == null && room.getStatus() != LiveQuizRoomStatus.OPEN) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_JOINABLE");
        }
        // Student da join truoc do co the goi lai /join de reconnect, tru PREPARING/CLOSED.
        if (existing != null && room.getStatus() == LiveQuizRoomStatus.PREPARING) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_JOINABLE");
        }
        if (existing != null && room.getStatus() == LiveQuizRoomStatus.CLOSED) {
            throw new ConflictException("LIVE_QUIZ_ROOM_CLOSED");
        }
        roomService.ensureRedisReady(room, 1);
        ExamPaperPoolDTO paperPool = requirePaperPool(room);
        LiveQuizParticipant participant = existing != null
                ? existing
                : createParticipant(room, studentId, username, paperPool);
        if (paperRepo.findByParticipantId(participant.getId()).isEmpty()) {
            // Paper duoc tao mot lan cho tung student de reconnect khong doi thu tu cau hoi.
            createPaper(room, participant, paperPool);
        }
        participant.setLastSeenAt(OffsetDateTime.now(clock));
        LiveQuizParticipant saved = participantRepo.save(participant);
        publishJoined(room, saved);
        return new StudentLiveQuizJoinResponseDTO(
                room.getId(),
                room.getExamId(),
                saved.getId(),
                room.getRoomCode(),
                room.getQuizTitle(),
                room.getSubjectName(),
                saved.getStatus(),
                room.getStatus(),
                saved.getTotalQuestions(),
                participantRepo.findByRoomId(room.getId()).size(),
                snapshotService.currentRank(room.getId(), saved.getId()),
                OffsetDateTime.now(clock)
        );
    }

    /**
     * Chuan hoa room code de user nhap hoa/thuong deu join duoc.
     */
    private String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("ROOM_CODE_REQUIRED");
        }
        return code.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Tim room tu code, uu tien Redis cache va fallback database khi can.
     */
    private LiveQuizRoom resolveRoom(String code) {
        String cachedRoomId = roomCache.getRoomIdByCode(code);
        if (cachedRoomId != null && !cachedRoomId.isBlank()) {
            // Duong nhanh: room code -> roomId tu Redis hot cache.
            UUID roomId = UUID.fromString(cachedRoomId);
            return roomRepo.findById(roomId)
                    .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_ROOM_NOT_FOUND"));
        }
        // Fallback DB giup join van hoat dong neu Redis vua mat code key.
        return roomRepo.findByRoomCode(code)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_ROOM_NOT_FOUND"));
    }

    /**
     * Lay paper pool va validate moi cau hoi co time limit hop le.
     */
    private ExamPaperPoolDTO requirePaperPool(LiveQuizRoom room) {
        ExamPaperPoolDTO paperPool = roomCache.getPaperPool(room.getId());
        if (paperPool == null || paperPool.questions() == null || paperPool.questions().isEmpty()) {
            throw new ConflictException("LIVE_QUIZ_PAPER_NOT_READY");
        }
        for (PaperQuestionDTO question : paperPool.questions()) {
            if (question.timeLimitSeconds() == null || question.timeLimitSeconds() <= 0) {
                throw new ConflictException("LIVE_QUIZ_INVALID_QUESTION_TIME_LIMIT");
            }
        }
        return paperPool;
    }

    /**
     * Tao participant o trang thai JOINED, chua bat dau timer cau hoi.
     */
    private LiveQuizParticipant createParticipant(
            LiveQuizRoom room,
            UUID studentId,
            String username,
            ExamPaperPoolDTO paperPool
    ) {
        LiveQuizParticipant participant = new LiveQuizParticipant();
        participant.setRoomId(room.getId());
        participant.setStudentId(studentId);
        participant.setStudentNameSnapshot(username != null && !username.isBlank() ? username : studentId.toString());
        participant.setStatus(LiveQuizParticipantStatus.JOINED);
        participant.setCurrentQuestionPosition(0);
        participant.setTotalQuestions(paperPool.questions().size());
        participant.setMaxScore(maxScore(paperPool));
        participant.setTotalScore(BigDecimal.ZERO);
        participant.setJoinedAt(OffsetDateTime.now(clock));
        participant.setLastSeenAt(OffsetDateTime.now(clock));
        return participantRepo.save(participant);
    }

    /**
     * Tao paper ca nhan cho participant va luu thu tu cau hoi co shuffle.
     */
    private void createPaper(LiveQuizRoom room, LiveQuizParticipant participant, ExamPaperPoolDTO paperPool) {
        long seed = random.nextLong();
        List<UUID> questionOrder = new ArrayList<>(paperPool.questions().stream()
                .map(PaperQuestionDTO::questionId)
                .toList());
        // Luu seed de co the audit/debug thu tu cau hoi da cap cho student.
        java.util.Collections.shuffle(questionOrder, new java.util.Random(seed));
        LiveQuizParticipantPaper paper = new LiveQuizParticipantPaper();
        paper.setRoomId(room.getId());
        paper.setParticipantId(participant.getId());
        paper.setStudentId(participant.getStudentId());
        paper.setSeed(seed);
        try {
            paper.setQuestionOrder(objectMapper.writeValueAsString(questionOrder));
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_QUESTION_ORDER_WRITE_FAILED", exception);
        }
        paperRepo.save(paper);
    }

    /**
     * Tinh tong diem toi da cua paper pool.
     */
    private BigDecimal maxScore(ExamPaperPoolDTO paperPool) {
        return paperPool.questions().stream()
                .map(question -> BigDecimal.valueOf(question.score()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Phat event realtime de teacher lobby thay student moi join/reconnect.
     */
    private void publishJoined(LiveQuizRoom room, LiveQuizParticipant participant) {
        realtimePublisher.publish(room.getId(), new LiveQuizRealtimeMessageDTO(
                room.getId(),
                room.getExamId(),
                "PARTICIPANT_JOINED",
                OffsetDateTime.now(clock),
                room.getStatus(),
                null,
                snapshotService.toParticipantSnapshot(participant, snapshotService.currentRank(room.getId(), participant.getId())),
                null,
                snapshotService.summary(participantRepo.findByRoomId(room.getId()))
        ));
    }
}
