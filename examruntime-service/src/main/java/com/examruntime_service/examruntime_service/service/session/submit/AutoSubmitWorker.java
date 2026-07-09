package com.examruntime_service.examruntime_service.service.session.submit;

import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(
        name = "examruntime.autosubmit.enabled",
        havingValue = "true",
        matchIfMissing = true
)
// Quet cac session qua deadline va dung chung finalizer voi submit thu cong.
public class AutoSubmitWorker {

    private static final Logger logger = LoggerFactory.getLogger(AutoSubmitWorker.class);

    private final ExamSessionRepo examSessionRepo;
    private final SubmissionFinalizationService finalizationService;
    private final Clock clock;
    private final int batchSize;

    public AutoSubmitWorker(
            ExamSessionRepo examSessionRepo,
            SubmissionFinalizationService finalizationService,
            Clock clock,
            @Value("${examruntime.autosubmit.batch-size:50}") int batchSize
    ) {
        this.examSessionRepo = examSessionRepo;
        this.finalizationService = finalizationService;
        this.clock = clock;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${examruntime.autosubmit.poll-delay-ms:10000}")
    public void submitTimedOutSessions() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        // Chi quet session IN_PROGRESS da qua deadline; moi batch gioi han de tranh giu lock qua lau.
        List<ExamSession> dueSessions = examSessionRepo.findAllByStatusAndServerDeadlineAtLessThanEqual(
                ExamSessionStatus.IN_PROGRESS,
                now,
                PageRequest.of(0, batchSize)
        );
        for (ExamSession session : dueSessions) {
            try {
                // Khong truyen studentId vi worker chay noi bo; finalizer van khoa session va chong duplicate.
                finalizationService.finalizeSubmission(session.getId(), null, SubmitReason.TIME_UP, null);
            } catch (Exception exception) {
                // Khong dung ca batch khi mot session loi; lan quet sau se retry neu session van con IN_PROGRESS.
                logger.warn("Auto submit failed for session {}", session.getId(), exception);
            }
        }
    }
}
