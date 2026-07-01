package com.examruntime_service.examruntime_service.model.entity;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "live_quiz_rooms",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_live_quiz_room_code", columnNames = "room_code")
        },
        indexes = {
                @Index(name = "idx_live_quiz_rooms_exam_status", columnList = "exam_id,status"),
                @Index(name = "idx_live_quiz_rooms_owner_status", columnList = "owner_teacher_id,status")
        }
)
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class LiveQuizRoom extends BaseEntity {

    @Column(name = "exam_id", nullable = false)
    private UUID examId;

    @Column(name = "room_code", nullable = false, length = 6)
    private String roomCode;

    @Column(name = "owner_teacher_id", nullable = false)
    private UUID ownerTeacherId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private LiveQuizRoomStatus status = LiveQuizRoomStatus.PREPARING;

    private OffsetDateTime openedAt;
    private OffsetDateTime startedAt;
    private OffsetDateTime closedAt;

    @Version
    private long version;
}
