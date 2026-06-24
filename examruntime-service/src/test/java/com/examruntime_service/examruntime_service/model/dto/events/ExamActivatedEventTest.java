package com.examruntime_service.examruntime_service.model.dto.events;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ExamActivatedEventTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().build();

    @Test
    void roundTripsProducerCompatibleJson() throws Exception {
        UUID eventId = UUID.fromString("8f2d65a2-c12b-4789-9a72-132d456bcdef");
        UUID examId = UUID.fromString("00000000-0000-0000-0000-000000000000");

        ExamActivatedEvent event = new ExamActivatedEvent(
                eventId,
                ExamActivatedEvent.EVENT_TYPE,
                ExamActivatedEvent.EVENT_VERSION,
                examId,
                1,
                OffsetDateTime.of(2026, 7, 1, 8, 0, 0, 0, ZoneOffset.ofHours(7)),
                OffsetDateTime.of(2026, 7, 1, 9, 0, 0, 0, ZoneOffset.ofHours(7)),
                15,
                10,
                OffsetDateTime.of(2026, 6, 24, 11, 0, 0, 0, ZoneOffset.ofHours(7))
        );

        String json = objectMapper.writeValueAsString(event);

        assertThat(json).contains("\"eventType\":\"ExamActivated\"");
        assertThat(json).contains("\"eventVersion\":1");
        assertThat(json).contains("\"joinBeforeMinutes\":15");
        assertThat(json).contains("\"joinAfterMinutes\":10");

        ExamActivatedEvent deserialized = objectMapper.readValue(json, ExamActivatedEvent.class);
        assertThat(deserialized.eventId()).isEqualTo(event.eventId());
        assertThat(deserialized.eventType()).isEqualTo(event.eventType());
        assertThat(deserialized.eventVersion()).isEqualTo(event.eventVersion());
        assertThat(deserialized.examId()).isEqualTo(event.examId());
        assertThat(deserialized.snapshotVersion()).isEqualTo(event.snapshotVersion());
        assertThat(deserialized.startAt().toInstant()).isEqualTo(event.startAt().toInstant());
        assertThat(deserialized.endAt().toInstant()).isEqualTo(event.endAt().toInstant());
        assertThat(deserialized.joinBeforeMinutes()).isEqualTo(event.joinBeforeMinutes());
        assertThat(deserialized.joinAfterMinutes()).isEqualTo(event.joinAfterMinutes());
        assertThat(deserialized.occurredAt().toInstant()).isEqualTo(event.occurredAt().toInstant());
    }
}
