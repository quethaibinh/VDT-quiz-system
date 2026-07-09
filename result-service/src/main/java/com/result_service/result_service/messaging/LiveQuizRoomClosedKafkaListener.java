package com.result_service.result_service.messaging;

import com.result_service.result_service.service.livequiz.LiveQuizResultIngestionService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Component
public class LiveQuizRoomClosedKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(LiveQuizRoomClosedKafkaListener.class);

    private final LiveQuizResultIngestionService ingestionService;

    public LiveQuizRoomClosedKafkaListener(LiveQuizResultIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @KafkaListener(
            topics = "${result.kafka.topic.live-quiz-results:live-quiz-result-events}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${result.kafka.enabled:true}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        // Ack sau khi ingestion thanh cong de Kafka retry neu DB/transaction gap loi.
        // Ingestion service tu xu ly duplicate nen retry an toan.
        LiveQuizResultIngestionService.IngestionOutcome outcome = ingestionService.handle(record.value());
        acknowledgment.acknowledge();
        if (outcome.processed()) {
            logger.info("Ingested LiveQuizRoomClosed eventId={} roomId={} insertedResults={}",
                    outcome.eventId(), outcome.roomId(), outcome.insertedResults());
        } else {
            logger.info("Skipped duplicate LiveQuizRoomClosed eventId={} roomId={}",
                    outcome.eventId(), outcome.roomId());
        }
    }
}
