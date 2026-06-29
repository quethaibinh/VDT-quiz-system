package com.result_service.result_service.repository;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamResultRepo extends JpaRepository<ExamResult, UUID> {
    boolean existsBySubmissionId(UUID submissionId);

    Optional<ExamResult> findBySubmissionId(UUID submissionId);

    Page<ExamResult> findByExamId(UUID examId, Pageable pageable);

    List<ExamResult> findByExamId(UUID examId);

    Optional<ExamResult> findByExamIdAndId(UUID examId, UUID id);

    Optional<ExamResult> findByExamIdAndStudentId(UUID examId, UUID studentId);

    List<ExamResult> findByStudentIdOrderByGradedAtDesc(UUID studentId);

    @Query("select r.submissionId from ExamResult r where r.submissionId in :submissionIds")
    List<UUID> findExistingSubmissionIds(@Param("submissionIds") Collection<UUID> submissionIds);
}
