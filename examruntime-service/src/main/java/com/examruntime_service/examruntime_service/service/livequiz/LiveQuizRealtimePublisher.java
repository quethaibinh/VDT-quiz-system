package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRealtimeMessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
public class LiveQuizRealtimePublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    /**
     * Inject Redis template va flag bat/tat pubsub live quiz.
     */
    public LiveQuizRealtimePublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${examruntime.livequiz.redis-pubsub.enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    /**
     * Publish message live quiz vao Redis channel cua room.
     */
    public void publish(UUID roomId, LiveQuizRealtimeMessageDTO message) {
        if (!enabled || roomId == null || message == null) {
            // Cho phep tat live quiz pubsub trong test/local ma khong anh huong DB transaction.
            return;
        }
        try {
            // Publish vao channel rieng cua live quiz, tach khoi kenh exam monitor hien tai.
            redisTemplate.convertAndSend(
                    LiveQuizDestinations.eventsChannel(roomId),
                    objectMapper.writeValueAsString(message)
            );
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_REALTIME_PUBLISH_FAILED", exception);
        }
    }
}
