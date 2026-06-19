package com.exam_service.exam_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(name = "exam_questions",
        uniqueConstraints = {
            @UniqueConstraint(
                    name = "uk_exam_question_exam_question",
                    columnNames = {"exam_id", "question_id"}
            ),
            @UniqueConstraint(
                    name = "uk_exam_question_exam_order",
                    columnNames = {"exam_id", "sort_order"}
            )
        })
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * Mo hinh du kien de luu cau hoi va dap an dong bang khi co luong activate.
 * Luong tao DRAFT hien tai chua ghi vao bang nay.
 */
public class ExamQuestion extends BaseEntity{

    private UUID examId;
    private UUID questionId;
    private Integer questionVersion;
    private Float score; // diem mac dinh cua 1 cau
    private int sortOrder;
    private boolean required = true;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "question_snapshot", columnDefinition = "jsonb")
    // Snapshot noi dung ngan viec sua ngan hang cau hoi lam doi de dang thi.
    private String questionSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_key_snapshot", columnDefinition = "jsonb")
    // Dap an dung chi dung noi bo cho cham diem, khong tra ve client lam bai.
    private String answerKeySnapshot;

}
