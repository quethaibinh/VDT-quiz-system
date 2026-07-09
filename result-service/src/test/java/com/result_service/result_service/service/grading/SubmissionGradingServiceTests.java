package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import com.result_service.result_service.model.entity.GradingJob;
import com.result_service.result_service.model.entity.InboxMessage;
import com.result_service.result_service.model.entity.enums.GradingJobStatus;
import com.result_service.result_service.model.entity.enums.InboxMessageStatus;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.GradingJobRepo;
import com.result_service.result_service.repository.InboxMessageRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class SubmissionGradingServiceTests {

    @Autowired
    private SubmissionGradingService gradingService;

    @Autowired
    private InboxMessageRepo inboxMessageRepo;

    @Autowired
    private GradingJobRepo gradingJobRepo;

    @Autowired
    private ExamResultRepo examResultRepo;

    @Autowired
    private ResultAnswerRepo resultAnswerRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID eventId;
    private UUID submissionId;
    private UUID sessionId;
    private UUID examId;
    private UUID studentId;
    private UUID q1;
    private UUID q2;
    private UUID q3;
    private UUID q1Correct;
    private UUID q2Correct;
    private UUID q2Wrong;
    private UUID q3Correct;

    @BeforeEach
    void setUp() {
        resultAnswerRepo.deleteAll();
        examResultRepo.deleteAll();
        gradingJobRepo.deleteAll();
        inboxMessageRepo.deleteAll();

        eventId = UUID.randomUUID();
        submissionId = UUID.randomUUID();
        sessionId = UUID.randomUUID();
        examId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        q1 = UUID.randomUUID();
        q2 = UUID.randomUUID();
        q3 = UUID.randomUUID();
        q1Correct = UUID.randomUUID();
        q2Correct = UUID.randomUUID();
        q2Wrong = UUID.randomUUID();
        q3Correct = UUID.randomUUID();
    }

    @Test
    void gradesSelectedOptionsAndBlankAnswersDeterministically() throws Exception {
        String rawJson = objectMapper.writeValueAsString(validEvent());

        SubmissionGradingService.GradingOutcome outcome = gradingService.handle(rawJson);

        assertThat(outcome.processed()).isTrue();
        assertThat(examResultRepo.count()).isEqualTo(1);
        assertThat(resultAnswerRepo.count()).isEqualTo(3);
        var result = examResultRepo.findBySubmissionId(submissionId).orElseThrow();
        assertThat(result.getTotalQuestions()).isEqualTo(3);
        assertThat(result.getAnsweredQuestions()).isEqualTo(2);
        assertThat(result.getCorrectCount()).isEqualTo(1);
        assertThat(result.getWrongCount()).isEqualTo(1);
        assertThat(result.getBlankCount()).isEqualTo(1);
        assertThat(result.getTotalScore()).isEqualByComparingTo(new BigDecimal("3.3333"));
        assertThat(result.getMaxScore()).isEqualByComparingTo(new BigDecimal("10.0000"));
        assertThat(result.getPercentage()).isEqualByComparingTo(new BigDecimal("33.333"));
        assertThat(result.getGraderVersion()).isEqualTo("normalized-10-v1");
        var correctAnswer = resultAnswerRepo.findAll().stream()
                .filter(answer -> answer.getQuestionId().equals(q1))
                .findFirst()
                .orElseThrow();
        assertThat(correctAnswer.getScoreAwarded()).isEqualByComparingTo(new BigDecimal("3.333"));
        assertThat(correctAnswer.getMaxScore()).isEqualByComparingTo(new BigDecimal("3.333"));
    }

    @Test
    void skipsDuplicateDeliveryWithoutCreatingDuplicateRows() throws Exception {
        String rawJson = objectMapper.writeValueAsString(validEvent());

        assertThat(gradingService.handle(rawJson).processed()).isTrue();
        assertThat(gradingService.handle(rawJson).processed()).isFalse();

        assertThat(inboxMessageRepo.count()).isEqualTo(1);
        assertThat(gradingJobRepo.count()).isEqualTo(1);
        assertThat(examResultRepo.count()).isEqualTo(1);
        assertThat(resultAnswerRepo.count()).isEqualTo(3);
        assertThat(inboxMessageRepo.findById(eventId).orElseThrow().getStatus())
                .isEqualTo(InboxMessageStatus.PROCESSED);
    }

    @Test
    void recordsFailedJobAndAllowsRetryWhenSnapshotBecomesValid() throws Exception {
        String invalid = objectMapper.writeValueAsString(eventWithQuestions(List.of()));

        assertThatThrownBy(() -> gradingService.handle(invalid))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("PAPER_SNAPSHOT_QUESTIONS_REQUIRED");

        assertThat(inboxMessageRepo.findById(eventId).orElseThrow().getStatus())
                .isEqualTo(InboxMessageStatus.FAILED);
        GradingJob failedJob = gradingJobRepo.findByMessageId(eventId).orElseThrow();
        assertThat(failedJob.getStatus()).isEqualTo(GradingJobStatus.FAILED);
        assertThat(failedJob.getErrorMessage()).isEqualTo("PAPER_SNAPSHOT_QUESTIONS_REQUIRED");

        String valid = objectMapper.writeValueAsString(validEvent());
        assertThat(gradingService.handle(valid).processed()).isTrue();

        assertThat(inboxMessageRepo.findById(eventId).orElseThrow().getStatus())
                .isEqualTo(InboxMessageStatus.PROCESSED);
        assertThat(gradingJobRepo.findByMessageId(eventId).orElseThrow().getStatus())
                .isEqualTo(GradingJobStatus.DONE);
        assertThat(examResultRepo.count()).isEqualTo(1);
    }

    private SubmissionCreatedEvent validEvent() {
        return eventWithQuestions(List.of(
                question(q1, 1, q1Correct, "2.0"),
                question(q2, 2, q2Correct, "3.0"),
                question(q3, 3, q3Correct, "2.0")
        ));
    }

    private SubmissionCreatedEvent eventWithQuestions(List<SubmissionCreatedEvent.QuestionSnapshot> questions) {
        return new SubmissionCreatedEvent(
                eventId,
                SubmissionCreatedEvent.EVENT_TYPE,
                "examruntime-service",
                OffsetDateTime.parse("2026-06-27T09:00:00Z"),
                submissionId,
                sessionId,
                examId,
                studentId,
                1,
                1,
                "STUDENT",
                OffsetDateTime.parse("2026-06-27T09:00:00Z"),
                List.of(
                        new SubmissionCreatedEvent.AnswerSnapshotItem(q1, List.of(q1Correct)),
                        new SubmissionCreatedEvent.AnswerSnapshotItem(q2, List.of(q2Wrong))
                ),
                new SubmissionCreatedEvent.PaperSnapshot(
                        questions,
                        Map.of("studentId", studentId),
                        Map.of("examId", examId)
                )
        );
    }

    private SubmissionCreatedEvent.QuestionSnapshot question(UUID questionId, int order, UUID correctOptionId, String maxScore) {
        return new SubmissionCreatedEvent.QuestionSnapshot(
                questionId,
                order,
                List.of(correctOptionId),
                new BigDecimal(maxScore),
                null,
                Map.of("questionId", questionId, "order", order)
        );
    }
}
