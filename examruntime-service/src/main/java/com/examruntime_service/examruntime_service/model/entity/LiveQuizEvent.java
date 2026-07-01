package com.examruntime_service.examruntime_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "live_quiz_events",
        indexes = {
                @Index(name = "idx_live_quiz_events_room_time", columnList = "room_id,occurred_at"),
                @Index(name = "idx_live_quiz_events_participant", columnList = "participant_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuizEvent extends BaseEntity { // bang nay nham muc dinh log nhung hanh dong trong luc sinh vien choi, nhung hien tai chua duoc su dung cho logic nào het

    @Column(name = "room_id", nullable = false)
    private UUID roomId;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false)
    private OffsetDateTime occurredAt;
}
