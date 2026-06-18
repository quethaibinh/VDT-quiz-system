package com.exam_service.exam_service.model.entity;

import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.HandleViolation;
import com.exam_service.exam_service.model.entity.enums.ShowResultPolicy;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "exam",
        indexes = {
            @Index(name="idx_teacher_status_start_at", columnList = "created_by_teacher_id,status,start_at"),
            @Index(name="idx_status_start_at", columnList = "status,start_at")
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Exam extends BaseEntity{

    private String code;
    private String title;
    private String description;
    private UUID subjectId;
    private String subjectNameSnapshot;
    private UUID createdByTeacherId;
    @Enumerated(EnumType.STRING)
    private ExamStatus status;
    private LocalDateTime startAt;
    private int durationMinutes;
    private int joinBeforeMinutes; // thoi gian cho vao truoc
    private int joinAfterMinutes; // thoi gian cho phep vao muon
    private boolean shuffleQuestions; // cho phep xao cau
    private boolean shuffleOptions; // cho phep xao dap an
    @Enumerated(EnumType.STRING)
    private ShowResultPolicy showResultPolicy = ShowResultPolicy.AFTER_CLOSED;
    private boolean autoSubmit;
    private boolean requireFullscreen = true; // lua chon giam sat cua giao vien
    private int maxViolationAllowed = 5; // nguong canh bao vi pham
    @Enumerated(EnumType.STRING)
    private HandleViolation handleViolation = HandleViolation.LOCK;
    private LocalDateTime activedAt;
    private LocalDateTime closedAt;

}
