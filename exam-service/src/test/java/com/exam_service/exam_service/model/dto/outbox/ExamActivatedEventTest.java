package com.exam_service.exam_service.model.dto.outbox;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExamActivatedEventTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    @Test
    void serializesCorrectlyWithTimezonePreservation() throws Exception {
        UUID eventId = UUID.fromString("8f2d65a2-c12b-4789-9a72-132d456bcdef");
        UUID examId = UUID.fromString("00000000-0000-0000-0000-000000000000");
        OffsetDateTime startAt = OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7));
        OffsetDateTime endAt = OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.ofHours(7));
        OffsetDateTime occurredAt = OffsetDateTime.of(2026, 7, 1, 7, 20, 0, 0, ZoneOffset.ofHours(7));

        ExamActivatedEvent event = new ExamActivatedEvent(
                eventId,
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                examId,
                1,
                startAt,
                endAt,
                10,
                15,
                occurredAt
        );

        String json = objectMapper.writeValueAsString(event);

        assertThat(json).contains("\"eventId\":\"8f2d65a2-c12b-4789-9a72-132d456bcdef\"");
        assertThat(json).contains("\"eventType\":\"ExamActivated\"");
        assertThat(json).contains("\"eventVersion\":1");
        assertThat(json).contains("\"examId\":\"00000000-0000-0000-0000-000000000000\"");
        assertThat(json).contains("\"snapshotVersion\":1");
        // Spring's default ObjectMapper serialization for OffsetDateTime will include the timezone properly
        // Depending on WRITE_DATES_AS_TIMESTAMPS, which is disabled by default in Spring Boot.
        assertThat(json).contains("\"startAt\":\"2026-07-01T08:00:00+07:00\"");
        assertThat(json).contains("\"endAt\":\"2026-07-01T09:00:00+07:00\"");
        assertThat(json).contains("\"joinBeforeMinutes\":10");
        assertThat(json).contains("\"joinAfterMinutes\":15");
        assertThat(json).contains("\"occurredAt\":\"2026-07-01T07:20:00+07:00\"");

        ExamActivatedEvent deserialized = objectMapper.readValue(json, ExamActivatedEvent.class);
        assertThat(deserialized.eventId()).isEqualTo(eventId);
        assertThat(deserialized.startAt().getOffset()).isEqualTo(ZoneOffset.ofHours(7));
    }
}
