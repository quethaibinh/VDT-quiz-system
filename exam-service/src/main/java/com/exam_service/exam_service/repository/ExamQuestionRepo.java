package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.ExamQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExamQuestionRepo extends JpaRepository<ExamQuestion, UUID> {
}
