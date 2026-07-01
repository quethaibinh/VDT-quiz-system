package com.examruntime_service.examruntime_service.config.redis;

import com.examruntime_service.examruntime_service.service.monitor.MonitorRedisSubscriber;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizRedisSubscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Dang ky Redis listener cho monitor va live quiz backplane.
 *
 * Hai pattern duoc cau hinh rieng de luong live quiz khong tron voi exam monitor.
 */
@Configuration
public class MonitorRedisPubSubConfig {

    /**
     * Tao adapter de Redis container goi MonitorRedisSubscriber.handleMessage.
     */
    @Bean
    public MessageListenerAdapter monitorMessageListenerAdapter(MonitorRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    /**
     * Tao adapter de Redis container goi LiveQuizRedisSubscriber.handleMessage.
     */
    @Bean
    public MessageListenerAdapter liveQuizMessageListenerAdapter(LiveQuizRedisSubscriber subscriber) {
        return new MessageListenerAdapter(subscriber, "handleMessage");
    }

    /**
     * Dang ky pattern listener cho exam monitor va live quiz trong cung container.
     */
    @Bean
    public RedisMessageListenerContainer monitorRedisMessageListenerContainer(
            RedisConnectionFactory connectionFactory,
            @Qualifier("monitorMessageListenerAdapter") MessageListenerAdapter monitorMessageListenerAdapter,
            @Qualifier("liveQuizMessageListenerAdapter") MessageListenerAdapter liveQuizMessageListenerAdapter,
            @Value("${examruntime.monitor.redis-pubsub.enabled:true}") boolean enabled,
            @Value("${examruntime.monitor.events-channel-pattern:exam:monitor:events:*}") String pattern,
            @Value("${examruntime.livequiz.redis-pubsub.enabled:true}") boolean liveQuizEnabled,
            @Value("${examruntime.livequiz.events-channel-pattern:livequiz:room:events:*}") String liveQuizPattern
    ) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        if (enabled) {
            // Pattern subscription giup khong can tao listener moi cho tung examId.
            container.addMessageListener(monitorMessageListenerAdapter, new PatternTopic(pattern));
        }
        if (liveQuizEnabled) {
            // Pattern live quiz rieng: livequiz:room:events:*.
            container.addMessageListener(liveQuizMessageListenerAdapter, new PatternTopic(liveQuizPattern));
        }
        return container;
    }
}
