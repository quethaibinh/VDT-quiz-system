package com.question_service.question_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(
        name = "question_collection",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_collection_owner_subject_name",
                columnNames = {"owner_teacher_id", "subject_id", "name"}
        ),
        indexes = {
                @Index(name = "idx_collection_subject_visibility_status",
                        columnList = "subject_id,visibility,status"),
                @Index(name = "idx_collection_owner_status",
                        columnList = "owner_teacher_id,status")
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class QuestionCollection extends BaseEntity {

    @Column(name = "subject_id", nullable = false)
    private UUID subjectId;
    @Column(name = "owner_teacher_id", nullable = false)
    private UUID ownerTeacherId;
    @Column(name = "name", nullable = false)
    private String name;
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false)
    private CollectionVisibility visibility;
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CollectionStatus status;

}
