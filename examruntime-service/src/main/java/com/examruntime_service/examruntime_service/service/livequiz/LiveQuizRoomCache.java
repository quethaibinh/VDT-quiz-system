package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Redis hot cache cho live quiz room.
 *
 * PostgreSQL van la source of truth cua snapshot. Redis chi giu du lieu nong de
 * student join/play va teacher realtime doc nhanh khi room da duoc prepare.
 */
@Component
public class LiveQuizRoomCache {

    private static final Duration ROOM_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Inject Redis template va ObjectMapper de doc/ghi snapshot dang JSON.
     */
    public LiveQuizRoomCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Ghi toan bo snapshot can thiet cho room vao Redis khi prepare/open/start.
     */
    public void putRoomSnapshot(LiveQuizRoom room, int snapshotVersion, ExamPaperPoolDTO paperPool, ExamAnswerKeyDTO answerKey) {
        try {
            redisTemplate.opsForValue().set(codeKey(room.getRoomCode()), room.getId().toString(), ROOM_TTL);
            redisTemplate.opsForValue().set(roomKey(room.getId()), objectMapper.writeValueAsString(roomPayload(room, snapshotVersion)), ROOM_TTL);
            redisTemplate.opsForValue().set(paperKey(room.getId()), objectMapper.writeValueAsString(paperPool), ROOM_TTL);
            redisTemplate.opsForValue().set(answerKeyKey(room.getId()), objectMapper.writeValueAsString(answerKey), ROOM_TTL);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_ROOM_CACHE_WRITE_FAILED", exception);
        }
    }

    /**
     * Kiem tra Redis da co du cac key bat buoc cho live quiz room hay chua.
     */
    public boolean hasRoomSnapshot(LiveQuizRoom room) {
        Boolean hasCode = redisTemplate.hasKey(codeKey(room.getRoomCode()));
        Boolean hasRoom = redisTemplate.hasKey(roomKey(room.getId()));
        Boolean hasPaper = redisTemplate.hasKey(paperKey(room.getId()));
        Boolean hasAnswerKey = redisTemplate.hasKey(answerKeyKey(room.getId()));
        // Can du ca 4 key vi join/play phu thuoc vao code lookup, room status, paper va answer key.
        return Boolean.TRUE.equals(hasRoom)
                && Boolean.TRUE.equals(hasCode)
                && Boolean.TRUE.equals(hasPaper)
                && Boolean.TRUE.equals(hasAnswerKey);
    }

    /**
     * Cap nhat payload room trong Redis khi status/timestamp thay doi.
     */
    public void putRoomStatus(LiveQuizRoom room, int snapshotVersion) {
        try {
            redisTemplate.opsForValue().set(roomKey(room.getId()), objectMapper.writeValueAsString(roomPayload(room, snapshotVersion)), ROOM_TTL);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_ROOM_CACHE_WRITE_FAILED", exception);
        }
    }

    /**
     * Lay roomId tu room code de join nhanh qua Redis.
     */
    public String getRoomIdByCode(String roomCode) {
        return redisTemplate.opsForValue().get(codeKey(roomCode));
    }

    /**
     * Doc paper pool cua room tu Redis de cap cau hoi cho student.
     */
    public ExamPaperPoolDTO getPaperPool(UUID roomId) {
        String payload = redisTemplate.opsForValue().get(paperKey(roomId));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, ExamPaperPoolDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_PAPER_CACHE_READ_FAILED", exception);
        }
    }

    /**
     * Doc answer key cua room tu Redis de cham diem server-side.
     */
    public ExamAnswerKeyDTO getAnswerKey(UUID roomId) {
        String payload = redisTemplate.opsForValue().get(answerKeyKey(roomId));
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, ExamAnswerKeyDTO.class);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_ANSWER_KEY_CACHE_READ_FAILED", exception);
        }
    }

    /**
     * Tao payload room nhe de luu status va metadata can cho runtime.
     */
    private Map<String, Object> roomPayload(LiveQuizRoom room, int snapshotVersion) {
        // Dung LinkedHashMap thay Map.of de chap nhan cac timestamp optional null.
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("roomId", room.getId());
        payload.put("examId", room.getExamId());
        payload.put("roomCode", room.getRoomCode());
        payload.put("ownerTeacherId", room.getOwnerTeacherId());
        payload.put("status", room.getStatus().name());
        payload.put("snapshotVersion", snapshotVersion);
        if (room.getOpenedAt() != null) {
            payload.put("openedAt", room.getOpenedAt());
        }
        if (room.getStartedAt() != null) {
            payload.put("startedAt", room.getStartedAt());
        }
        if (room.getClosedAt() != null) {
            payload.put("closedAt", room.getClosedAt());
        }
        return payload;
    }

    /**
     * Redis key anh xa room code sang roomId.
     */
    public static String codeKey(String roomCode) {
        return "livequiz:code:%s".formatted(roomCode);
    }

    /**
     * Redis key luu metadata room.
     */
    public static String roomKey(UUID roomId) {
        return "livequiz:room:%s".formatted(roomId);
    }

    /**
     * Redis key luu paper pool cua room.
     */
    public static String paperKey(UUID roomId) {
        return "livequiz:room:%s:paper".formatted(roomId);
    }

    /**
     * Redis key luu answer key cua room.
     */
    public static String answerKeyKey(UUID roomId) {
        return "livequiz:room:%s:answer-key".formatted(roomId);
    }
}
