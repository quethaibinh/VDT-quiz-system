package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.repository.OutboxEventRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "exam.outbox.enabled=false",
        "exam.activation.enabled=false"
})
class ExamActivationTransactionServiceTests {

    @Autowired
    private ExamRepo examRepo;

    @Autowired
    private OutboxEventRepo outboxRepo;

    @Autowired
    private ExamActivationTransactionService activationService;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<?, ?> kafkaTemplate;

    // Use a fixed clock to have deterministic tests
    @Autowired
    private Clock clock;

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-06-23T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @AfterEach
    void tearDown() {
        examRepo.deleteAll();
        outboxRepo.deleteAll();
    }

    private Exam createScheduledExam(int joinBeforeMinutes, OffsetDateTime startAt) {
        Exam exam = new Exam();
        exam.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8));
        exam.setTitle("Test Exam");
        exam.setSubjectId(UUID.randomUUID());
        exam.setCreatedByTeacherId(UUID.randomUUID());
        exam.setStatus(ExamStatus.SCHEDULED);
        exam.setStartAt(startAt);
        exam.setDurationMinutes(60);
        exam.setJoinBeforeMinutes(joinBeforeMinutes);
        exam.setCollectionId(UUID.randomUUID());
        return examRepo.saveAndFlush(exam);
    }

    @Test
    void activatesDueScheduledExamAndCreatesOutboxEvent() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        // threshold = startAt - joinBeforeMinutes - 30m
        // If startAt = now + 40m, and joinBeforeMinutes = 10, threshold = now
        Exam exam = createScheduledExam(10, now.plusMinutes(40));

        boolean activated = activationService.activateIfDue(exam.getId());

        assertThat(activated).isTrue();

        Exam updated = examRepo.findById(exam.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ExamStatus.ACTIVE);
        assertThat(updated.getActivatedAt()).isNotNull();

        var events = outboxRepo.findAll();
        assertThat(events).hasSize(1);
        var event = events.get(0);
        assertThat(event.getEventType()).isEqualTo("ExamActivated");
        assertThat(event.getAggregateId()).isEqualTo(exam.getId());
    }

    @Test
    void doesNotActivateExamBeforeItsThreshold() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        // If startAt = now + 41m, joinBeforeMinutes = 10, threshold = now + 1m > now
        Exam exam = createScheduledExam(10, now.plusMinutes(41));

        boolean activated = activationService.activateIfDue(exam.getId());

        assertThat(activated).isFalse();

        Exam updated = examRepo.findById(exam.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ExamStatus.SCHEDULED);
        assertThat(outboxRepo.count()).isZero();
    }

    @Test
    void activatesLateScheduledExamStillBeforeStartAt() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        // startAt = now + 5m (threshold was in the past)
        Exam exam = createScheduledExam(10, now.plusMinutes(5));

        boolean activated = activationService.activateIfDue(exam.getId());

        assertThat(activated).isTrue();
        Exam updated = examRepo.findById(exam.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ExamStatus.ACTIVE);
    }

    @Test
    void ignoresExamsNotInScheduledStatus() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exam exam = createScheduledExam(10, now.plusMinutes(40));
        exam.setStatus(ExamStatus.DRAFT);
        exam = examRepo.saveAndFlush(exam);

        boolean activated = activationService.activateIfDue(exam.getId());
        assertThat(activated).isFalse();

        exam.setStatus(ExamStatus.CLOSED);
        exam = examRepo.saveAndFlush(exam);
        assertThat(activationService.activateIfDue(exam.getId())).isFalse();
    }
}
