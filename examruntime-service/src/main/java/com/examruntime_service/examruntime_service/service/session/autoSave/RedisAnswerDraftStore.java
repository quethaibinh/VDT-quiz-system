package com.examruntime_service.examruntime_service.service.session.autoSave;

import com.examruntime_service.examruntime_service.model.dto.session.AnswerDraftRecord;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
// Redis la hot path luu draft answer da duoc backend validate.
public class RedisAnswerDraftStore implements AnswerDraftStore {

    public static final String DIRTY_SESSIONS_KEY = "runtime:answers:dirty-sessions";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisAnswerDraftStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    // luu autosave answer len redis
    public AnswerDraftSaveResult saveBatch(
            ExamSession session,
            List<AutosaveAnswerDTO> answers,
            long clientSeq,
            OffsetDateTime now,
            Duration ttl
    ) {
        UUID sessionId = session.getId();
        long serverSeq = nextServerSeq(sessionId, session.getAutosaveSeq());
        int savedCount = 0;
        int skippedCount = 0;

        for (AutosaveAnswerDTO answer : answers) {
            AnswerDraftRecord existing = readRecord(sessionId, answer.getQuestionId());
            if (existing != null && existing.getClientSeq() > clientSeq) {
                skippedCount++;
                continue;
            }

            AnswerDraftRecord record = AnswerDraftRecord.builder()
                    .questionId(answer.getQuestionId())
                    .selectedOptionIds(answer.getSelectedOptionIds() != null ? answer.getSelectedOptionIds() : List.of())
                    .answerText(answer.getAnswerText())
                    .markedForReview(answer.isMarkedForReview())
                    .clientSeq(clientSeq)
                    .serverSeq(serverSeq)
                    .serverReceivedAt(now)
                    .build();

            redisTemplate.opsForHash().put(answersKey(sessionId), answer.getQuestionId().toString(), serialize(record));
            savedCount++;
        }

        redisTemplate.opsForHash().put(stateKey(sessionId), "lastAutosaveAt", now.toString());
        redisTemplate.opsForHash().put(stateKey(sessionId), "autosaveSeq", Long.toString(serverSeq));
        int answeredCount = countAnswered(sessionId);
        redisTemplate.opsForHash().put(stateKey(sessionId), "answeredCount", Integer.toString(answeredCount));
        redisTemplate.opsForSet().add(DIRTY_SESSIONS_KEY, sessionId.toString());
        redisTemplate.expire(answersKey(sessionId), ttl);
        redisTemplate.expire(stateKey(sessionId), ttl);

        return AnswerDraftSaveResult.builder()
                .savedCount(savedCount)
                .skippedCount(skippedCount)
                .answeredCount(answeredCount)
                .serverSeq(serverSeq)
                .lastAutosaveAt(now)
                .build();
    }

    @Override
    // lay het nhung snapshort cau hoi va dap an da chon cua sinh vien o redis de resume
    public List<AnswerDraftRecord> findAll(UUID sessionId) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(answersKey(sessionId));
        if (entries == null || entries.isEmpty()) {
            return List.of();
        }

        List<AnswerDraftRecord> records = new ArrayList<>();
        for (Object value : entries.values()) {
            if (value instanceof String payload) {
                records.add(deserialize(payload));
            }
        }
        return records;
    }

    @Override
    // neu redis loi thi chay ham nay de day data vua fallback tu checkpoint database len lai redis
    public void hydrate(ExamSession session, List<AnswerDraftRecord> records, OffsetDateTime now, Duration ttl) {
        if (records == null || records.isEmpty()) {
            return;
        }
        UUID sessionId = session.getId();
        long maxServerSeq = session.getAutosaveSeq();
        for (AnswerDraftRecord record : records) {
            maxServerSeq = Math.max(maxServerSeq, record.getServerSeq());
            redisTemplate.opsForHash().put(answersKey(sessionId), record.getQuestionId().toString(), serialize(record));
        }
        redisTemplate.opsForHash().put(stateKey(sessionId), "lastAutosaveAt", now.toString());
        redisTemplate.opsForHash().put(stateKey(sessionId), "autosaveSeq", Long.toString(maxServerSeq));
        redisTemplate.opsForHash().put(stateKey(sessionId), "answeredCount", Long.toString(countAnsweredRecords(records)));
        redisTemplate.expire(answersKey(sessionId), ttl);
        redisTemplate.expire(stateKey(sessionId), ttl);
    }

    @Override
    public Set<UUID> findDirtySessionIds() {
        Set<String> values = redisTemplate.opsForSet().members(DIRTY_SESSIONS_KEY);
        if (values == null || values.isEmpty()) {
            return Set.of();
        }
        Set<UUID> ids = new LinkedHashSet<>();
        for (String value : values) {
            ids.add(UUID.fromString(value));
        }
        return ids;
    }

    @Override
    public void clearDirtySession(UUID sessionId) {
        redisTemplate.opsForSet().remove(DIRTY_SESSIONS_KEY, sessionId.toString());
    }

    public static String answersKey(UUID sessionId) {
        return "session:%s:answers".formatted(sessionId);
    }

    public static String stateKey(UUID sessionId) {
        return "session:%s:state".formatted(sessionId);
    }

    private long nextServerSeq(UUID sessionId, long sessionSeq) {
        Object current = redisTemplate.opsForHash().get(stateKey(sessionId), "autosaveSeq");
        long redisSeq = 0;
        if (current instanceof String value && !value.isBlank()) {
            redisSeq = Long.parseLong(value);
        }
        return Math.max(sessionSeq, redisSeq) + 1;
    }

    private AnswerDraftRecord readRecord(UUID sessionId, UUID questionId) {
        Object value = redisTemplate.opsForHash().get(answersKey(sessionId), questionId.toString());
        if (value instanceof String payload) {
            return deserialize(payload);
        }
        return null;
    }

    private int countAnswered(UUID sessionId) {
        return countAnsweredRecords(findAll(sessionId));
    }

    private int countAnsweredRecords(List<AnswerDraftRecord> records) {
        return (int) records.stream()
                .filter(record -> (record.getSelectedOptionIds() != null && !record.getSelectedOptionIds().isEmpty())
                        || (record.getAnswerText() != null && !record.getAnswerText().isBlank()))
                .count();
    }

    private String serialize(AnswerDraftRecord record) {
        try {
            return objectMapper.writeValueAsString(record);
        } catch (Exception exception) {
            throw new IllegalStateException("ANSWER_DRAFT_SERIALIZATION_FAILED", exception);
        }
    }

    private AnswerDraftRecord deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, AnswerDraftRecord.class);
        } catch (Exception exception) {
            throw new IllegalStateException("ANSWER_DRAFT_DESERIALIZATION_FAILED", exception);
        }
    }
}
