package com.question_service.question_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "topic")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Topic extends BaseEntity {

    private UUID subjectId;
    @Column(name = "name", nullable = false)
    private String name;
    private String description;
    @Column(name = "slug", nullable = false)
    private String slug;
    private UUID parentTopicId;

}
