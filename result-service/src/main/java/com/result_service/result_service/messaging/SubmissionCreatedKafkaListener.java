package com.result_service.result_service.messaging;

import com.result_service.result_service.service.grading.SubmissionGradingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class SubmissionCreatedKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionCreatedKafkaListener.class);

    private final SubmissionGradingService gradingService;

    public SubmissionCreatedKafkaListener(SubmissionGradingService gradingService) {
        this.gradingService = gradingService;
    }

    @KafkaListener(
            topics = "${result.kafka.topic.runtime-submissions}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${result.kafka.enabled:true}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        SubmissionGradingService.GradingOutcome outcome = gradingService.handle(record.value());
        acknowledgment.acknowledge();

        if (outcome.processed()) {
            logger.info("Graded SubmissionCreated eventId={} submissionId={}",
                    outcome.eventId(), outcome.submissionId());
        } else {
            logger.info("Skipped duplicate SubmissionCreated eventId={} submissionId={}",
                    outcome.eventId(), outcome.submissionId());
        }
    }
}
