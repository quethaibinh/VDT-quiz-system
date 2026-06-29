package com.exam_service.exam_service.service.exams;

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
public class ExamClosingScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ExamClosingScheduler.class);

    private final ExamRepo examRepo;
    private final ExamClosingTransactionService closingTransactionService;
    private final Clock clock;

    @Value("${exam.closing.enabled:true}")
    private boolean enabled;

    @Value("${exam.closing.batch-size:100}")
    private int batchSize;

    @Value("${exam.closing.grace-period-seconds:60}")
    private long gracePeriodSeconds;

    public ExamClosingScheduler(
            ExamRepo examRepo,
            ExamClosingTransactionService closingTransactionService,
            Clock clock
    ) {
        this.examRepo = examRepo;
        this.closingTransactionService = closingTransactionService;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${exam.closing.poll-delay-ms:60000}")
    public void closeEndedExams() {
        if (!enabled) {
            return;
        }

        OffsetDateTime closeBefore = OffsetDateTime.now(clock).minusSeconds(Math.max(0, gracePeriodSeconds));
        var candidateIds = examRepo.findCandidateIdsForClosing(
                closeBefore,
                PageRequest.of(0, batchSize)
        );

        if (candidateIds.isEmpty()) {
            return;
        }

        int successCount = 0;
        int errorCount = 0;
        for (UUID examId : candidateIds.getContent()) {
            try {
                boolean closed = closingTransactionService.closeIfEnded(examId, gracePeriodSeconds);
                if (closed) {
                    successCount++;
                }
            } catch (Exception exception) {
                logger.error("Failed to process closing for exam {}", examId, exception);
                errorCount++;
            }
        }

        if (successCount > 0 || errorCount > 0) {
            logger.info("Exam closing scan completed. closed={}, errors={}", successCount, errorCount);
        }
    }
}
