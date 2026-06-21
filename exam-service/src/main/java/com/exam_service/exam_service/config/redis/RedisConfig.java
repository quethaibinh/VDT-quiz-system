package com.exam_service.exam_service.config.redis;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key dung String de Runtime va Result doc cung mot contract.
        template.setKeySerializer(new StringRedisSerializer());

        // Bean nay giu tuong thich cho cac cache object khac; snapshot dung StringRedisTemplate.
        template.setValueSerializer(GenericJacksonJsonRedisSerializer.builder().build());

        template.afterPropertiesSet();
        return template;
    }

}
