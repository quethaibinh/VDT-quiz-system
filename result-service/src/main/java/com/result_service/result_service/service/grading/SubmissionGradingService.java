package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.GradingJob;
import com.result_service.result_service.model.entity.InboxMessage;
import com.result_service.result_service.model.entity.ResultAnswer;
import com.result_service.result_service.model.entity.enums.ExamStatus;
import com.result_service.result_service.model.entity.enums.GradingJobStatus;
import com.result_service.result_service.model.entity.enums.InboxMessageStatus;
import com.result_service.result_service.model.entity.enums.ResultType;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.GradingJobRepo;
import com.result_service.result_service.repository.InboxMessageRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import com.result_service.result_service.service.results.ResultPolicyResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Service dieu phoi toan bo quy trinh cham diem cho mot submission duoc gui toi.
 * Quy trinh bao gom: parse event, kiem tra trung lap (idempotency), tinh diem,
 * luu ket qua vao DB (ExamResult, ResultAnswer) va ghi nhan trang thai job (GradingJob).
 */
@Service
public class SubmissionGradingService {

    public static final String GRADER_VERSION = "normalized-10-v1";

    private final SubmissionEventParser eventParser;
    private final GradingJsonSupport jsonSupport;
    private final GradeCalculator gradeCalculator;
    private final InboxMessageRepo inboxMessageRepo;
    private final GradingJobRepo gradingJobRepo;
    private final ExamResultRepo examResultRepo;
    private final ResultAnswerRepo resultAnswerRepo;
    private final GradingFailureRecorder failureRecorder;
    private final ResultPolicyResolver policyResolver;
    private final Clock clock;

    public SubmissionGradingService(
            SubmissionEventParser eventParser,
            GradingJsonSupport jsonSupport,
            GradeCalculator gradeCalculator,
            InboxMessageRepo inboxMessageRepo,
            GradingJobRepo gradingJobRepo,
            ExamResultRepo examResultRepo,
            ResultAnswerRepo resultAnswerRepo,
            GradingFailureRecorder failureRecorder,
            ResultPolicyResolver policyResolver,
            Clock clock
    ) {
        this.eventParser = eventParser;
        this.jsonSupport = jsonSupport;
        this.gradeCalculator = gradeCalculator;
        this.inboxMessageRepo = inboxMessageRepo;
        this.gradingJobRepo = gradingJobRepo;
        this.examResultRepo = examResultRepo;
        this.resultAnswerRepo = resultAnswerRepo;
        this.failureRecorder = failureRecorder;
        this.policyResolver = policyResolver;
        this.clock = clock;
    }

    /**
     * Phuong thuc xu ly chinh cho viec cham diem. Chay trong mot Transaction.
     * Neu xay ra loi, ghi nhan thong tin loi vao he thong va thoi bay loi ra ngoai de transaction rollback.
     *
     * @param rawJson chuoi JSON nguyen ban nhan tu Kafka
     * @return GradingOutcome ket qua xu ly (moi hay trung lap)
     */
    @Transactional
    public GradingOutcome handle(String rawJson) {
        SubmissionCreatedEvent event = eventParser.parse(rawJson);
        String payloadHash = jsonSupport.sha256(rawJson);
        try {
            return grade(event, rawJson, payloadHash);
        } catch (RuntimeException exception) {
            // Ghi nhan thong tin that bai de phuc vu debug truoc khi nem lai exception
            failureRecorder.recordFailure(event, rawJson, payloadHash, exception);
            throw exception;
        }
    }

    /**
     * Thuc hien cac buoc cham diem chi tiet va cap nhat database.
     */
    private GradingOutcome grade(SubmissionCreatedEvent event, String rawJson, String payloadHash) {
        // 1. Kiem tra idempotency dua tren eventId trong InboxMessage va ton tai cua ExamResult
        // Dieu nay giup dam bao neu Kafka redeliver tin nhan thi he thong khong bi cham diem hai lan.
        InboxMessage existingInbox = inboxMessageRepo.findById(event.eventId()).orElse(null);
        if (existingInbox != null
                && existingInbox.getStatus() == InboxMessageStatus.PROCESSED
                && examResultRepo.existsBySubmissionId(event.submissionId())) {
            return GradingOutcome.duplicate(event.eventId(), event.submissionId());
        }
        if (examResultRepo.existsBySubmissionId(event.submissionId())) {
            markInboxProcessed(existingInbox, event, payloadHash);
            return GradingOutcome.duplicate(event.eventId(), event.submissionId());
        }

        OffsetDateTime startedAt = OffsetDateTime.now(clock);
        
        // 2. Goi bo tinh diem GradeCalculator de thuc hien validate va cham diem
        GradeComputation computed = gradeCalculator.compute(event);

        // 3. Khoi tao hoac cap nhat ban tin InboxMessage sang trang thai PROCESSING
        InboxMessage inbox = existingInbox != null ? existingInbox : new InboxMessage();
        inbox.setMessageId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setProducer(event.producer());
        inbox.setReceivedAt(existingInbox != null ? existingInbox.getReceivedAt() : OffsetDateTime.now(clock));
        inbox.setStatus(InboxMessageStatus.PROCESSING);
        inbox.setPayloadHash(payloadHash);
        inboxMessageRepo.saveAndFlush(inbox);

        // 4. Luu thong tin GradingJob lam lich su thuc thi va so lan thu (attempt count) de ho tro debug.
        GradingJob job = gradingJobRepo.findByMessageId(event.eventId()).orElseGet(GradingJob::new);
        job.setMessageId(event.eventId());
        job.setSubmissionId(event.submissionId());
        job.setExamId(event.examId());
        job.setStudentId(event.studentId());
        job.setStatus(GradingJobStatus.PROCESSING);
        job.setAttemptCount(job.getAttemptCount() + 1);
        job.setStartedAt(OffsetDateTime.now(clock));
        job.setPayload(rawJson);
        job.setErrorCode(null);
        job.setErrorMessage(null);
        gradingJobRepo.saveAndFlush(job);

        // 5. Build va luu ket qua thi (ExamResult) vao database
        ExamResult result = buildResult(event, computed, startedAt);
        examResultRepo.saveAndFlush(result);
        
        // 6. Luu danh sach chi tiet cau tra loi (ResultAnswer) lien ket voi ExamResult vua tao
        for (ResultAnswer answer : computed.answers()) {
            answer.setResult(result);
            resultAnswerRepo.save(answer);
        }

        // 7. Cap nhat trang thai Job hoan thanh (DONE)
        job.setStatus(GradingJobStatus.DONE);
        job.setFinishedAt(OffsetDateTime.now(clock));
        gradingJobRepo.save(job);

        // 8. Cap nhat trang thai InboxMessage thanh hoan thanh (PROCESSED)
        inbox.setStatus(InboxMessageStatus.PROCESSED);
        inbox.setProcessedAt(OffsetDateTime.now(clock));
        inboxMessageRepo.save(inbox);
        
        return GradingOutcome.processed(event.eventId(), event.submissionId());
    }

    /**
     * Danh dau ban ghi inbox la da xu ly xong (PROCESSED) khi phat hien event trung lap.
     */
    private void markInboxProcessed(InboxMessage inbox, SubmissionCreatedEvent event, String payloadHash) {
        InboxMessage target = inbox != null ? inbox : new InboxMessage();
        target.setMessageId(event.eventId());
        target.setEventType(event.eventType());
        target.setProducer(event.producer());
        target.setReceivedAt(inbox != null ? inbox.getReceivedAt() : OffsetDateTime.now(clock));
        target.setStatus(InboxMessageStatus.PROCESSED);
        target.setProcessedAt(OffsetDateTime.now(clock));
        target.setPayloadHash(payloadHash);
        inboxMessageRepo.save(target);
    }

    /**
     * Map thong tin tu event va ket qua tinh diem sang entity ExamResult.
     */
    private ExamResult buildResult(SubmissionCreatedEvent event, GradeComputation computed, OffsetDateTime startedAt) {
        OffsetDateTime gradedAt = OffsetDateTime.now(clock);
        ExamResult result = new ExamResult();
        result.setResultType(ResultType.STANDARD_EXAM);
        result.setExamId(event.examId());
        result.setStudentId(event.studentId());
        result.setSessionId(event.sessionId());
        result.setSubmissionId(event.submissionId());
        result.setAttemptNo(event.attemptNo());
        result.setStatus(ExamStatus.GRADED);
        result.setTotalQuestions(computed.totalQuestions());
        result.setAnsweredQuestions(computed.answeredQuestions());
        result.setCorrectCount(computed.correctCount());
        result.setWrongCount(computed.wrongCount());
        result.setBlankCount(computed.blankCount());
        result.setTotalScore(computed.totalScore());
        result.setMaxScore(computed.maxScore());
        result.setPercentage(computed.percentage());
        result.setSubmittedAt(event.submittedAt());
        result.setGradedAt(gradedAt);
        result.setGradingDurationMs(Math.toIntExact(Duration.between(startedAt, gradedAt).toMillis()));
        result.setGraderVersion(GRADER_VERSION);
        
        // Luu lai snapshot thong tin hoc sinh tai thoi diem nop bai duoi dang JSON
        result.setStudentSnapshot(jsonSupport.toJson(event.paperSnapshot().studentSnapshot() != null
                ? event.paperSnapshot().studentSnapshot()
                : Map.of("studentId", event.studentId())));
                
        // Luu lai snapshot thong tin ky thi tai thoi diem nop bai duoi dang JSON
        result.setExamSnapshot(jsonSupport.toJson(event.paperSnapshot().examSnapshot() != null
                ? event.paperSnapshot().examSnapshot()
                : Map.of("examId", event.examId(), "snapshotVersion", event.snapshotVersion())));
                
        // Xac dinh trang thai review ban dau cua ket qua dua tren showResultPolicy tu metadata cua exam
        result.setReviewStatus(policyResolver.initialReviewStatus(result));
        
        // Neu trang thai review la RELEASED (cong bo ngay), luu thoi gian cong bo bang thoi gian cham
        if (result.getReviewStatus() == com.result_service.result_service.model.entity.enums.ResultReviewStatus.RELEASED) {
            result.setReleasedAt(gradedAt);
        }
        return result;
    }

    /**
     * Record ghi nhan ket qua cua mot lan handle event tu listener.
     */
    public record GradingOutcome(boolean processed, UUID eventId, UUID submissionId) {
        static GradingOutcome processed(UUID eventId, UUID submissionId) {
            return new GradingOutcome(true, eventId, submissionId);
        }

        static GradingOutcome duplicate(UUID eventId, UUID submissionId) {
            return new GradingOutcome(false, eventId, submissionId);
        }
    }
}
