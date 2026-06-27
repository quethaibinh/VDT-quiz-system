package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.OutboxStatus;
import com.exam_service.exam_service.model.entity.enums.QuestionDifficulty;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class JsonPersistenceTests {

    @Autowired
    private ExamQuestionRepo questionRepo;

    @Autowired
    private OutboxEventRepo outboxRepo;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<?, ?> kafkaTemplate;

    @Test
    void persistsSnapshotAndOutboxJsonThroughHibernateFormatMapper() {
        ExamQuestion question = new ExamQuestion();
        question.setExamId(UUID.randomUUID());
        question.setQuestionId(UUID.randomUUID());
        question.setQuestionVersion(1);
        question.setDifficulty(QuestionDifficulty.EASY);
        question.setScore(1.0F);
        question.setSortOrder(0);
        question.setRequired(true);
        question.setQuestionSnapshot("""
                {"content":"1 + 1 = ?","options":[{"key":"A","content":"2"}]}
                """);
        question.setAnswerKeySnapshot("""
                {"correctOptionIds":["00000000-0000-0000-0000-000000000001"]}
                """);

        ExamQuestion savedQuestion = questionRepo.saveAndFlush(question);

        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("EXAM");
        event.setAggregateId(question.getExamId());
        event.setEventType("EXAM_SNAPSHOT_CACHE_REQUESTED");
        event.setPayload("""
                {"snapshotVersion":1}
                """);
        event.setStatus(OutboxStatus.PENDING);

        OutboxEvent savedEvent = outboxRepo.saveAndFlush(event);

        assertThat(savedQuestion.getId()).isNotNull();
        assertThat(savedEvent.getId()).isNotNull();
    }

    @Test
    void persistsExamActivatedOutboxJson() {
        OutboxEvent event = new OutboxEvent();
        event.setAggregateType("EXAM");
        event.setAggregateId(UUID.randomUUID());
        event.setEventType("EXAM_ACTIVATED");
        event.setPayload("""
                {
                  "eventId": "8f2d65a2-c12b-4789-9a72-132d456bcdef",
                  "eventType": "ExamActivated",
                  "eventVersion": 1,
                  "examId": "00000000-0000-0000-0000-000000000000",
                  "snapshotVersion": 1,
                  "startAt": "2026-07-01T08:00:00+07:00",
                  "endAt": "2026-07-01T09:00:00+07:00",
                  "joinBeforeMinutes": 10,
                  "joinAfterMinutes": 15,
                  "occurredAt": "2026-07-01T07:20:00+07:00"
                }
                """);
        event.setStatus(OutboxStatus.PENDING);

        OutboxEvent savedEvent = outboxRepo.saveAndFlush(event);
        assertThat(savedEvent.getId()).isNotNull();
    }
}
