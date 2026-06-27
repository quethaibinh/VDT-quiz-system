package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.GradingJob;
import com.result_service.result_service.model.entity.InboxMessage;
import com.result_service.result_service.model.entity.ResultAnswer;
import com.result_service.result_service.model.entity.enums.ExamStatus;
import com.result_service.result_service.model.entity.enums.GradingJobStatus;
import com.result_service.result_service.model.entity.enums.InboxMessageStatus;
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

@Service
public class SubmissionGradingService {

    public static final String GRADER_VERSION = "selected-options-v1";

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

    @Transactional
    public GradingOutcome handle(String rawJson) {
        SubmissionCreatedEvent event = eventParser.parse(rawJson);
        String payloadHash = jsonSupport.sha256(rawJson);
        try {
            return grade(event, rawJson, payloadHash);
        } catch (RuntimeException exception) {
            failureRecorder.recordFailure(event, rawJson, payloadHash, exception);
            throw exception;
        }
    }

    private GradingOutcome grade(SubmissionCreatedEvent event, String rawJson, String payloadHash) {
        // Kiem tra idempotency truoc khi tao result de Kafka co the retry an toan.
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
        GradeComputation computed = gradeCalculator.compute(event);

        InboxMessage inbox = existingInbox != null ? existingInbox : new InboxMessage();
        inbox.setMessageId(event.eventId());
        inbox.setEventType(event.eventType());
        inbox.setProducer(event.producer());
        inbox.setReceivedAt(existingInbox != null ? existingInbox.getReceivedAt() : OffsetDateTime.now(clock));
        inbox.setStatus(InboxMessageStatus.PROCESSING);
        inbox.setPayloadHash(payloadHash);
        inboxMessageRepo.saveAndFlush(inbox);

        // GradingJob luu payload goc va so lan thu de debug duoc cac lan retry.
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

        ExamResult result = buildResult(event, computed, startedAt);
        examResultRepo.saveAndFlush(result);
        for (ResultAnswer answer : computed.answers()) {
            answer.setResult(result);
            resultAnswerRepo.save(answer);
        }

        job.setStatus(GradingJobStatus.DONE);
        job.setFinishedAt(OffsetDateTime.now(clock));
        gradingJobRepo.save(job);

        inbox.setStatus(InboxMessageStatus.PROCESSED);
        inbox.setProcessedAt(OffsetDateTime.now(clock));
        inboxMessageRepo.save(inbox);
        return GradingOutcome.processed(event.eventId(), event.submissionId());
    }

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

    private ExamResult buildResult(SubmissionCreatedEvent event, GradeComputation computed, OffsetDateTime startedAt) {
        OffsetDateTime gradedAt = OffsetDateTime.now(clock);
        ExamResult result = new ExamResult();
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
        result.setStudentSnapshot(jsonSupport.toJson(event.paperSnapshot().studentSnapshot() != null
                ? event.paperSnapshot().studentSnapshot()
                : Map.of("studentId", event.studentId())));
        result.setExamSnapshot(jsonSupport.toJson(event.paperSnapshot().examSnapshot() != null
                ? event.paperSnapshot().examSnapshot()
                : Map.of("examId", event.examId(), "snapshotVersion", event.snapshotVersion())));
        result.setReviewStatus(policyResolver.initialReviewStatus(result));
        if (result.getReviewStatus() == com.result_service.result_service.model.entity.enums.ResultReviewStatus.RELEASED) {
            result.setReleasedAt(gradedAt);
        }
        return result;
    }

    public record GradingOutcome(boolean processed, UUID eventId, UUID submissionId) {
        static GradingOutcome processed(UUID eventId, UUID submissionId) {
            return new GradingOutcome(true, eventId, submissionId);
        }

        static GradingOutcome duplicate(UUID eventId, UUID submissionId) {
            return new GradingOutcome(false, eventId, submissionId);
        }
    }
}
