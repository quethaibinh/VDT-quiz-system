package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.ExamAssignment;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamAssignmentRepo extends JpaRepository<ExamAssignment, UUID> {

    Optional<ExamAssignment> findByExamIdAndStudentId(UUID examId, UUID studentId);

    List<ExamAssignment> findAllByExamIdAndStudentIdIn(
            UUID examId,
            Collection<UUID> studentIds
    );

    Page<ExamAssignment> findAllByExamIdAndStatus(
            UUID examId,
            AssignmentStatus status,
            Pageable pageable
    );

    long countByExamIdAndStatus(UUID examId, AssignmentStatus status);
}
