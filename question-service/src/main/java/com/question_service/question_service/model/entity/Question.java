package com.question_service.question_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "question")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Question extends BaseEntity {

    private UUID subjectId;
    private UUID topicId;
    private UUID ownerTeacherId;
    @Column(name = "question_type", nullable = false)
    private String questionType;
    @Column(name = "content", nullable = false)
    private String content;
    @Enumerated(EnumType.STRING)
    private ContentFormat contentFormat = ContentFormat.PLAIN_TEXT;
    private String explanation;
    @Enumerated(EnumType.STRING)
    @Column(name = "difficulty", nullable = false)
    private Difficulty difficulty;
    private Double defaultScore;
    private int estimatedSecond;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private QuestionVisibility visibility = QuestionVisibility.PRIVATE;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QuestionStatus status = QuestionStatus.ACTIVE;
    @Column(name = "source")
    @Enumerated(EnumType.STRING)
    private Source source;
    private UUID importJobId;
    private String metadata;


}
