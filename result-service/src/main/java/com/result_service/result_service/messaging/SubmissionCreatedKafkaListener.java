package com.result_service.result_service.messaging;

import com.result_service.result_service.service.grading.SubmissionGradingService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

/**
 * Listener lang nghe cac su kien tu Kafka topic lien quan den viec tao submission.
 * Khi co tin nhan moi, class nay se kich hoat tien trinh cham diem.
 */
@Component
public class SubmissionCreatedKafkaListener {

    private static final Logger logger = LoggerFactory.getLogger(SubmissionCreatedKafkaListener.class);

    private final SubmissionGradingService gradingService;

    /**
     * Constructor injection de khoi tao SubmissionGradingService.
     */
    public SubmissionCreatedKafkaListener(SubmissionGradingService gradingService) {
        this.gradingService = gradingService;
    }

    /**
     * Phuong thuc tiep nhan message tu Kafka topic.
     * Sau khi goi gradingService de cham diem, tien hanh gui Acknowledgment de xac nhan da xu ly tin nhan thanh cong.
     *
     * @param record record tu Kafka chua chuoi JSON cua SubmissionCreatedEvent
     * @param acknowledgment doi tuong dung de commit offset thu cong trong Kafka
     */
    @KafkaListener(
            topics = "${result.kafka.topic.runtime-submissions}",
            groupId = "${spring.kafka.consumer.group-id}",
            autoStartup = "${result.kafka.enabled:true}"
    )
    public void onMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        // Goi service thuc hien cham diem va tra ve ket qua (da xu ly moi hay skip trung lap)
        SubmissionGradingService.GradingOutcome outcome = gradingService.handle(record.value());
        
        // Xac nhan voi Kafka rang da nhan va xu ly xong tin nhan (commit offset)
        acknowledgment.acknowledge();

        // Ghi log trang thai dua tren viec co thuc su cham diem hay skip do trung lap
        if (outcome.processed()) {
            logger.info("Graded SubmissionCreated eventId={} submissionId={}",
                    outcome.eventId(), outcome.submissionId());
        } else {
            logger.info("Skipped duplicate SubmissionCreated eventId={} submissionId={}",
                    outcome.eventId(), outcome.submissionId());
        }
    }
}
