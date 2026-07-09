package com.exam_service.exam_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    @ConditionalOnProperty(
            name = "exam.kafka.topic.auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    public NewTopic examLifecycleTopic(
            @Value("${exam.kafka.topic.exam-lifecycle:exam-lifecycle-events}") String topicName,
            @Value("${exam.kafka.topic.exam-lifecycle.partitions:3}") int partitions,
            @Value("${exam.kafka.topic.exam-lifecycle.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
