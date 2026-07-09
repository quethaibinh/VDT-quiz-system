package com.question_service.question_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "question_collection_item",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collection_question",
                columnNames = {"collection_id", "question_id"}
        ),
        indexes = {
                @Index(name = "idx_collection_item_collection", columnList = "collection_id"),
                @Index(name = "idx_collection_item_question", columnList = "question_id")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionCollectionItem extends BaseEntity{

    @Column(name = "collection_id", nullable = false)
    private UUID collectionId;
    @Column(name = "question_id", nullable = false)
    private UUID questionId;

}
