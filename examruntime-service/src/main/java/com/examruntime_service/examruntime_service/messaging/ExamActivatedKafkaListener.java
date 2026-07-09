package com.examruntime_service.examruntime_service.messaging;

import com.examruntime_service.examruntime_service.model.dto.events.ExamActivatedEvent;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationWarmupService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
/**
 * Bien Kafka record thanh loi goi business thuan. Class nay khong xu ly Redis truc tiep
 * de ack Kafka chi phu thuoc vao ket qua cua RuntimeActivationWarmupService.
 */
public class ExamActivatedKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(ExamActivatedKafkaListener.class);

    private final RuntimeActivationWarmupService warmupService;
    private final ObjectMapper objectMapper;

    public ExamActivatedKafkaListener(RuntimeActivationWarmupService warmupService, ObjectMapper objectMapper) {
        this.warmupService = warmupService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${examruntime.kafka.topic.exam-lifecycle}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${examruntime.kafka.enabled:true}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        ExamActivatedEvent event = parseForLog(record.value());
        logger.info("Received ExamActivated candidate eventId={} examId={}",
                event == null ? null : event.eventId(),
                event == null ? null : event.examId());

        RuntimeActivationWarmupService.WarmupResult result = warmupService.handle(record.value());
        // Neu handle nem exception, dong nay khong chay va Spring Kafka se retry record.
        acknowledgment.acknowledge();

        if (result.processed()) {
            logger.info("Runtime activation ready examId={} source={}", result.examId(), result.snapshotSource());
        } else {
            logger.info("Skipped duplicate ExamActivated examId={}", result.examId());
        }
    }

    private ExamActivatedEvent parseForLog(String rawJson) {
        try {
            return objectMapper.readValue(rawJson, ExamActivatedEvent.class);
        } catch (Exception exception) {
            // Payload loi van de service parse/validate lai de nem loi chuan va khong ack.
            logger.warn("Received invalid ExamActivated payload shape");
            return null;
        }
    }
}
