package com.examruntime_service.examruntime_service.config.kafka;

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
            name = "examruntime.kafka.topic.auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    public NewTopic runtimeSubmissionTopic(
            @Value("${examruntime.kafka.topic.submission:runtime-submission-events}") String topicName,
            @Value("${examruntime.kafka.topic.submission.partitions:3}") int partitions,
            @Value("${examruntime.kafka.topic.submission.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }

    @Bean
    @ConditionalOnProperty(
            name = "examruntime.kafka.topic.auto-create",
            havingValue = "true",
            matchIfMissing = true
    )
    public NewTopic liveQuizResultTopic(
            @Value("${examruntime.kafka.topic.live-quiz-result:live-quiz-result-events}") String topicName,
            @Value("${examruntime.kafka.topic.live-quiz-result.partitions:3}") int partitions,
            @Value("${examruntime.kafka.topic.live-quiz-result.replication-factor:1}") short replicationFactor
    ) {
        return TopicBuilder.name(topicName)
                .partitions(partitions)
                .replicas(replicationFactor)
                .build();
    }
}
