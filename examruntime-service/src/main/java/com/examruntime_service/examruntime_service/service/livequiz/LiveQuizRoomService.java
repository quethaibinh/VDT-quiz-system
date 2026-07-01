package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.CreateLiveQuizRoomRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRoomDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

/**
 * Quan ly phong live quiz. Chua tao participant/session trong phase nay.
 *
 * Room duoc tao o PREPARING sau prepare, sau do giao vien goi open rieng.
 * Mot exam chi co mot room active tai mot thoi diem de retry prepare khong sinh
 * nhieu ma phong khac nhau.
 */
@Service
public class LiveQuizRoomService {

    private static final List<LiveQuizRoomStatus> ACTIVE_ROOM_STATUSES =
            List.of(LiveQuizRoomStatus.PREPARING, LiveQuizRoomStatus.OPEN, LiveQuizRoomStatus.STARTED);

    private final LiveQuizRoomRepo roomRepo;
    private final LiveQuizRoomCodeGenerator codeGenerator;
    private final ExamServiceSnapshotClient snapshotClient;
    private final LiveQuizRoomCache roomCache;
    private final LiveQuizRealtimePublisher realtimePublisher;

    /**
     * Inject repository, snapshot client, cache va realtime publisher cho room lifecycle.
     */
    public LiveQuizRoomService(
            LiveQuizRoomRepo roomRepo,
            LiveQuizRoomCodeGenerator codeGenerator,
            ExamServiceSnapshotClient snapshotClient,
            LiveQuizRoomCache roomCache,
            LiveQuizRealtimePublisher realtimePublisher
    ) {
        this.roomRepo = roomRepo;
        this.codeGenerator = codeGenerator;
        this.snapshotClient = snapshotClient;
        this.roomCache = roomCache;
        this.realtimePublisher = realtimePublisher;
    }

    /**
     * Tao room PREPARING hoac tra ve room active da co cho cung exam.
     */
    @Transactional
    public LiveQuizRoomDTO createRoom(CreateLiveQuizRoomRequestDTO request) {
        validateCreateRequest(request);
        // Idempotent theo examId: neu room PREPARING/OPEN da ton tai thi tra lai room do.
        return roomRepo.findFirstByExamIdAndStatusIn(request.examId(), ACTIVE_ROOM_STATUSES)
                .map(room -> hydrateAndReturn(room, request.snapshotVersion()))
                .orElseGet(() -> hydrateAndReturn(createNewRoom(request), request.snapshotVersion()));
    }

    /**
     * Lay room theo id va dam bao teacher dang dang nhap la owner.
     */
    @Transactional(readOnly = true)
    public LiveQuizRoomDTO get(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = requireOwned(roomId, teacherId);
        return toDto(room);
    }

    /**
     * Chuyen room tu PREPARING sang OPEN de student duoc join lobby.
     */
    @Transactional
    public LiveQuizRoomDTO open(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = requireOwned(roomId, teacherId);
        if (room.getStatus() == LiveQuizRoomStatus.OPEN) {
            // Open lap lai khong doi state, giup UI retry an toan.
            ensureRedisReady(room, 1);
            return toDto(room);
        }
        if (room.getStatus() != LiveQuizRoomStatus.PREPARING) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_OPENABLE");
        }
        ensureRedisReady(room, 1);
        room.setStatus(LiveQuizRoomStatus.OPEN);
        room.setOpenedAt(OffsetDateTime.now());
        LiveQuizRoom saved = roomRepo.save(room);
        roomCache.putRoomStatus(saved, 1);
        return toDto(saved);
    }

    /**
     * Dong room tu PREPARING/OPEN/STARTED sang CLOSED.
     */
    @Transactional
    public LiveQuizRoomDTO close(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = requireOwned(roomId, teacherId);
        if (room.getStatus() == LiveQuizRoomStatus.CLOSED) {
            // Close lap lai cung idempotent de nut UI/HTTP retry khong gay loi.
            return toDto(room);
        }
        if (room.getStatus() != LiveQuizRoomStatus.PREPARING
                && room.getStatus() != LiveQuizRoomStatus.OPEN
                && room.getStatus() != LiveQuizRoomStatus.STARTED) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_CLOSABLE");
        }
        room.setStatus(LiveQuizRoomStatus.CLOSED);
        room.setClosedAt(OffsetDateTime.now());
        return toDto(roomRepo.save(room));
    }

    /**
     * Bat dau room, khoa join moi va phat event ROOM_STARTED.
     */
    @Transactional
    public LiveQuizRoomDTO start(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = requireOwned(roomId, teacherId);
        if (room.getStatus() == LiveQuizRoomStatus.STARTED) {
            // Start duoc goi lai se tra ve state hien tai, khong phat lai event bat dau.
            ensureRedisReady(room, 1);
            return toDto(room);
        }
        if (room.getStatus() != LiveQuizRoomStatus.OPEN) {
            throw new ConflictException("LIVE_QUIZ_ROOM_NOT_STARTABLE");
        }
        ensureRedisReady(room, 1);
        // OPEN -> STARTED la ranh gioi khoa join muon va mo API lam bai.
        room.setStatus(LiveQuizRoomStatus.STARTED);
        room.setStartedAt(OffsetDateTime.now());
        LiveQuizRoom saved = roomRepo.save(room);
        roomCache.putRoomStatus(saved, 1);
        // Bao cho lobby/progress biet room da bat dau qua kenh live quiz rieng.
        realtimePublisher.publish(saved.getId(), new LiveQuizRealtimeMessageDTO(
                saved.getId(),
                saved.getExamId(),
                "ROOM_STARTED",
                OffsetDateTime.now(),
                saved.getStatus(),
                null,
                null,
                null,
                null
        ));
        return toDto(saved);
    }

    /**
     * Tao entity room moi voi ma phong unique va trang thai PREPARING.
     */
    private LiveQuizRoom createNewRoom(CreateLiveQuizRoomRequestDTO request) {
        LiveQuizRoom room = new LiveQuizRoom();
        room.setExamId(request.examId());
        room.setOwnerTeacherId(request.ownerTeacherId());
        room.setRoomCode(generateUniqueCode());
        // Tao room o PREPARING, chua cho hoc sinh join cho den khi teacher goi open.
        room.setStatus(LiveQuizRoomStatus.PREPARING);
        return roomRepo.saveAndFlush(room);
    }

    /**
     * Validate request prepare room tu Exam Service truoc khi tao room runtime.
     */
    private void validateCreateRequest(CreateLiveQuizRoomRequestDTO request) {
        // Phase nay chi ho tro CODE_ONLY; cac policy khac se them sau khi co join/play.
        if (!"CODE_ONLY".equals(request.joinPolicy().trim().toUpperCase(Locale.ROOT))) {
            throw new IllegalArgumentException("INVALID_LIVE_QUIZ_JOIN_POLICY");
        }
        if (request.snapshotVersion() < 1) {
            throw new IllegalArgumentException("INVALID_LIVE_QUIZ_SNAPSHOT_VERSION");
        }
    }

    /**
     * Sinh room code va retry neu trung unique constraint trong database.
     */
    private String generateUniqueCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = codeGenerator.generate();
            // Check toan bo bang vi roomCode co unique constraint, khong chi check active room.
            if (!roomRepo.existsByRoomCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("LIVE_QUIZ_ROOM_CODE_GENERATION_FAILED");
    }

    /**
     * Hydrate snapshot vao Redis roi map room sang DTO tra ve API.
     */
    private LiveQuizRoomDTO hydrateAndReturn(LiveQuizRoom room, int snapshotVersion) {
        hydrateRoomSnapshot(room, snapshotVersion);
        return toDto(room);
    }

    /**
     * Dam bao Redis co room snapshot, paper pool va answer key truoc khi join/play.
     */
    public void ensureRedisReady(LiveQuizRoom room, int snapshotVersion) {
        if (!roomCache.hasRoomSnapshot(room)) {
            // Redis co the mat key/TTL het han; hydrate lai tu Exam Service snapshot.
            hydrateRoomSnapshot(room, snapshotVersion);
        }
    }

    /**
     * Lay paper pool va answer key tu Exam Service roi ghi vao Redis hot cache.
     */
    private void hydrateRoomSnapshot(LiveQuizRoom room, int snapshotVersion) {
        ExamPaperPoolDTO paperPool = snapshotClient.getPaperPool(room.getExamId());
        ExamAnswerKeyDTO answerKey = snapshotClient.getAnswerKey(room.getExamId());
        if (paperPool.snapshotVersion() != snapshotVersion || answerKey.snapshotVersion() != snapshotVersion) {
            throw new IllegalStateException("LIVE_QUIZ_SNAPSHOT_VERSION_MISMATCH");
        }
        roomCache.putRoomSnapshot(room, snapshotVersion, paperPool, answerKey);
    }

    /**
     * Tim room va check ownerTeacherId de bao ve cac API teacher.
     */
    public LiveQuizRoom requireOwned(UUID roomId, UUID teacherId) {
        LiveQuizRoom room = roomRepo.findById(roomId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_ROOM_NOT_FOUND"));
        if (!room.getOwnerTeacherId().equals(teacherId)) {
            // Khong cho giao vien khac mo/dong phong khong thuoc ve minh.
            throw new UnauthorizedException("LIVE_QUIZ_ROOM_FORBIDDEN");
        }
        return room;
    }

    /**
     * Map entity LiveQuizRoom sang DTO public cho frontend/API.
     */
    private LiveQuizRoomDTO toDto(LiveQuizRoom room) {
        return new LiveQuizRoomDTO(
                room.getId(),
                room.getExamId(),
                room.getRoomCode(),
                room.getOwnerTeacherId(),
                room.getStatus(),
                room.getOpenedAt(),
                room.getStartedAt(),
                room.getClosedAt()
        );
    }
}
