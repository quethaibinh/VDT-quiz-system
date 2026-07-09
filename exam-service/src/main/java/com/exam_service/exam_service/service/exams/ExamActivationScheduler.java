package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
// class len lich tim xem co ban ghi nao du dieu kien de active khong (status=schedule, startAt - joinBefore - 30p <= now <= startAt)
public class ExamActivationScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExamActivationScheduler.class);

    private final ExamRepo examRepo;
    private final ExamActivationTransactionService activationTransactionService;
    private final Clock clock;

    @Value("${exam.activation.enabled:false}")
    private boolean enabled;

    @Value("${exam.activation.batch-size:100}")
    private int batchSize;

    @Value("${exam.activation.preload-lead-minutes:30}")
    private int preloadLeadMinutes;

    public ExamActivationScheduler(ExamRepo examRepo, ExamActivationTransactionService activationTransactionService, Clock clock) {
        this.examRepo = examRepo;
        this.activationTransactionService = activationTransactionService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${exam.activation.poll-delay-ms:60000}")
    public void activateExams() {
        if (!enabled) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now(clock);
        // Lay rong ung vien theo startAt, sau do transaction service se tinh nguong chinh xac theo tung ca thi.
        // Cong them 120 phut de bao phu joinBeforeMinutes lon ma van giu batch co gioi han.
        OffsetDateTime windowEnd = now.plusMinutes(preloadLeadMinutes).plusMinutes(120);

        var candidateIds = examRepo.findCandidateIdsForActivation(
                ExamStatus.SCHEDULED,
                windowEnd,
                PageRequest.of(0, batchSize)
        );

        if (candidateIds.isEmpty()) {
            return;
        }

        int successCount = 0;
        int errorCount = 0;

        for (UUID examId : candidateIds.getContent()) {
            try {
                boolean activated = activationTransactionService.activateIfDue(examId);
                if (activated) {
                    successCount++;
                }
            } catch (Exception e) {
                logger.error("Failed to process activation for exam {}", examId, e);
                errorCount++;
            }
        }

        if (successCount > 0 || errorCount > 0) {
            logger.info("Exam activation scan completed. activated={}, errors={}", successCount, errorCount);
        }
    }
}
