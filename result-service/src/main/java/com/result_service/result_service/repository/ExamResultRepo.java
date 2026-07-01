package com.result_service.result_service.repository;

import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.enums.ResultType;
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

    @Query("""
            select r from ExamResult r
            where r.resultType = com.result_service.result_service.model.entity.enums.ResultType.STANDARD_EXAM
              and r.examId = :examId
            """)
    Page<ExamResult> findByExamId(@Param("examId") UUID examId, Pageable pageable);

    @Query("""
            select r from ExamResult r
            where r.resultType = com.result_service.result_service.model.entity.enums.ResultType.STANDARD_EXAM
              and r.examId = :examId
            """)
    List<ExamResult> findByExamId(@Param("examId") UUID examId);

    @Query("""
            select r from ExamResult r
            where r.resultType = com.result_service.result_service.model.entity.enums.ResultType.STANDARD_EXAM
              and r.examId = :examId
              and r.id = :id
            """)
    Optional<ExamResult> findByExamIdAndId(@Param("examId") UUID examId, @Param("id") UUID id);

    @Query("""
            select r from ExamResult r
            where r.resultType = com.result_service.result_service.model.entity.enums.ResultType.STANDARD_EXAM
              and r.examId = :examId
              and r.studentId = :studentId
            """)
    Optional<ExamResult> findByExamIdAndStudentId(@Param("examId") UUID examId, @Param("studentId") UUID studentId);

    @Query("""
            select r from ExamResult r
            where r.resultType = com.result_service.result_service.model.entity.enums.ResultType.STANDARD_EXAM
              and r.studentId = :studentId
            order by r.gradedAt desc
            """)
    List<ExamResult> findByStudentIdOrderByGradedAtDesc(@Param("studentId") UUID studentId);

    Page<ExamResult> findByResultTypeAndRoomId(ResultType resultType, UUID roomId, Pageable pageable);

    List<ExamResult> findByResultTypeAndRoomId(ResultType resultType, UUID roomId);

    @Query("select r.submissionId from ExamResult r where r.submissionId in :submissionIds")
    List<UUID> findExistingSubmissionIds(@Param("submissionIds") Collection<UUID> submissionIds);
}
