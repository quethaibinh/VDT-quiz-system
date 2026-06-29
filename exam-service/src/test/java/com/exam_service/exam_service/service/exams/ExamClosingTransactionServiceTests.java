package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "exam.outbox.enabled=false",
        "exam.activation.enabled=false",
        "exam.closing.enabled=false"
})
class ExamClosingTransactionServiceTests {

    @Autowired
    private ExamRepo examRepo;

    @Autowired
    private ExamClosingTransactionService closingService;

    @Autowired
    private Clock clock;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

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
    }

    @Test
    void closesActiveExamAfterEndAtAndGracePeriod() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exam exam = saveExam(ExamStatus.ACTIVE, now.minusMinutes(5));

        boolean closed = closingService.closeIfEnded(exam.getId(), 60);

        assertThat(closed).isTrue();
        Exam updated = examRepo.findById(exam.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ExamStatus.CLOSED);
        assertThat(updated.getClosedAt()).isEqualTo(now);
    }

    @Test
    void doesNotCloseBeforeGracePeriodPasses() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exam exam = saveExam(ExamStatus.ACTIVE, now.minusSeconds(30));

        boolean closed = closingService.closeIfEnded(exam.getId(), 60);

        assertThat(closed).isFalse();
        Exam updated = examRepo.findById(exam.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(ExamStatus.ACTIVE);
        assertThat(updated.getClosedAt()).isNull();
    }

    @Test
    void ignoresExamNotActive() {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exam scheduled = saveExam(ExamStatus.SCHEDULED, now.minusMinutes(5));
        Exam cancelled = saveExam(ExamStatus.CANCELLED, now.minusMinutes(5));
        Exam alreadyClosed = saveExam(ExamStatus.CLOSED, now.minusMinutes(5));

        assertThat(closingService.closeIfEnded(scheduled.getId(), 60)).isFalse();
        assertThat(closingService.closeIfEnded(cancelled.getId(), 60)).isFalse();
        assertThat(closingService.closeIfEnded(alreadyClosed.getId(), 60)).isFalse();

        assertThat(examRepo.findById(scheduled.getId()).orElseThrow().getStatus()).isEqualTo(ExamStatus.SCHEDULED);
        assertThat(examRepo.findById(cancelled.getId()).orElseThrow().getStatus()).isEqualTo(ExamStatus.CANCELLED);
        assertThat(examRepo.findById(alreadyClosed.getId()).orElseThrow().getStatus()).isEqualTo(ExamStatus.CLOSED);
    }

    private Exam saveExam(ExamStatus status, OffsetDateTime endAt) {
        Exam exam = new Exam();
        exam.setCode("CODE-" + UUID.randomUUID().toString().substring(0, 8));
        exam.setTitle("Test Exam");
        exam.setSubjectId(UUID.randomUUID());
        exam.setCreatedByTeacherId(UUID.randomUUID());
        exam.setStatus(status);
        exam.setStartAt(endAt.minusHours(1));
        exam.setEndAt(endAt);
        exam.setDurationMinutes(60);
        exam.setJoinBeforeMinutes(10);
        exam.setCollectionId(UUID.randomUUID());
        return examRepo.saveAndFlush(exam);
    }
}
