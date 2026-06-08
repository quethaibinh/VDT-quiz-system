package com.question_service.question_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "question_option")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionOption extends BaseEntity {

    private UUID questionId;
    @Enumerated(EnumType.STRING)
    @Column(name = "option_key", nullable = false)
    private OptionKey optionKey;
    @Column(name = "content", nullable = false)
    private String content;
    @Enumerated(EnumType.STRING)
    private ContentFormat contentFormat = ContentFormat.PLAIN_TEXT;
    private boolean isCorrect;
    private String explanation;
    private String metadata;

}
