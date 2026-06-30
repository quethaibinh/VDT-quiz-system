package com.examruntime_service.examruntime_service.config.redis;

import com.examruntime_service.examruntime_service.service.monitor.MonitorRedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
/**
 * Dang ky Redis listener cho monitor backplane.
 *
 * RedisMessageListenerContainer lang nghe pattern `exam:monitor:events:*`.
 * Moi message nhan duoc se goi MonitorRedisSubscriber.handleMessage(String).
 */
public class MonitorRedisPubSubConfig {

    @Bean
    public MessageListenerAdapter monitorMessageListenerAdapter(MonitorRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    @Bean
    public RedisMessageListenerContainer monitorRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter monitorMessageListenerAdapter,
            @Value("${examruntime.monitor.redis-pubsub.enabled:true}") boolean enabled,
            @Value("${examruntime.monitor.events-channel-pattern:exam:monitor:events:*}") String pattern
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        if (enabled) {
            // Pattern subscription giup khong can tao listener moi cho tung examId.
            container.addMessageListener(monitorMessageListenerAdapter, new PatternTopic(pattern));
        }
        return container;
    }
}
