package com.exam_service.exam_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "exam_question")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ExamQuestion extends BaseEntity{

    private UUID examId;
    private UUID questionId;
    private Float score; // diem mac dinh cua 1 cau
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_snapshot", columnDefinition = "jsonb")
    private String questionSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key_snapshot", columnDefinition = "jsonb")
    private String answerKeySnapshot;

}
