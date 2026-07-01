package com.exam_service.exam_service.model.entity;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.model.entity.enums.HandleViolation;
import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;
import com.exam_service.exam_service.model.entity.enums.ShowResultPolicy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "exams",
        uniqueConstraints = {
            @UniqueConstraint(name = "uk_exam_code", columnNames = "code")
        },
        indexes = {
            @Index(name="idx_teacher_type_status_start_at", columnList = "created_by_teacher_id,exam_type,status,start_at"),
            @Index(name="idx_type_status_start_at", columnList = "exam_type,status,start_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Luu cau hinh va blueprint cua mot ca thi.
 * Khi con DRAFT, entity chi giu collection va quota, chua sinh ExamQuestion.
 */
public class Exam extends BaseEntity{

    @Column(nullable = false, length = 64)
    private String code;
    @Column(nullable = false)
    private String title;
    @Column(columnDefinition = "text")
    private String description;
    @Column(nullable = false)
    private UUID subjectId;
    // Snapshot phuc vu lich su va bao cao neu ten mon hoc thay doi.
    private String subjectNameSnapshot;
    @Column(nullable = false)
    private UUID createdByTeacherId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ExamStatus status;
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_type", nullable = false, length = 32, columnDefinition = "varchar(32) default 'STANDARD_EXAM'")
    private ExamType examType = ExamType.STANDARD_EXAM;
    private OffsetDateTime startAt;
    private OffsetDateTime endAt;
    private Integer durationMinutes;
    private int joinBeforeMinutes = 10;
    private int joinAfterMinutes;
    @Column(nullable = false)
    private UUID collectionId;
    // Snapshot ten bo cau hoi; collection goc van la source of truth cho quota khi sua.
    private String collectionNameSnapshot;
    private int easyCount;
    private int mediumCount;
    private int hardCount;
    private boolean shuffleQuestions;
    private boolean shuffleOptions;
    private Boolean liveQuizShowLeaderboard;
    private Boolean liveQuizShowCorrectAnswer = false;
    private Boolean liveQuizShuffleQuestions = true;
    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private LiveQuizJoinPolicy liveQuizJoinPolicy = LiveQuizJoinPolicy.CODE_ONLY;
    @Enumerated(EnumType.STRING)
    private ShowResultPolicy showResultPolicy = ShowResultPolicy.AFTER_CLOSED;
    private boolean autoSubmit = true;
    private boolean requireFullscreen = true;
    private int maxViolationAllowed = 5; // Nguong ap dung chinh sach xu ly vi pham.
    @Enumerated(EnumType.STRING)
    private HandleViolation handleViolation = HandleViolation.LOCK;
    private OffsetDateTime activatedAt;
    // Thoi diem giao vien chot lich; khac voi activatedAt cua runtime sau nay.
    private OffsetDateTime scheduledAt;
    @Column(nullable = false, columnDefinition = "integer default 0")
    private int snapshotVersion;
    private OffsetDateTime closedAt;

    @Version
    // Ngan hai request sua ca thi cung luc ghi de len nhau.
    private long version;

}
