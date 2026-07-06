package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorRealtimeMessageDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

@Service
/**
 * Publish monitor event vao Redis Pub/Sub.
 *
 * Moi exam co channel rieng: exam:monitor:events:{examId}.
 * Instance nao cung subscribe pattern nay; instance dang giu teacher WebSocket se forward ra STOMP topic.
 */
public class MonitorEventPublisher {

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public MonitorEventPublisher(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            @Value("${examruntime.monitor.redis-pubsub.enabled:true}") boolean enabled
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public void publish(UUID examId, MonitorRealtimeMessageDTO message) {
        if (!enabled) {
            // Cho phep tat Pub/Sub trong test/local debug ma khong anh huong DB state.
            return;
        }
        try {
            // Message JSON nho, chi dung de thong bao realtime; khong phai audit log ben vung.
            redisTemplate.convertAndSend(MonitorStateService.examEventsChannel(examId),
                    objectMapper.writeValueAsString(message));
        } catch (Exception exception) {
            throw new IllegalStateException("MONITOR_EVENT_PUBLISH_FAILED", exception);
        }
    }
}
