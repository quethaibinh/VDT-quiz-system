package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.util.Optional;
import java.util.UUID;

public interface ExamRepo extends JpaRepository<Exam, UUID>, JpaSpecificationExecutor<Exam> {

    Optional<Exam> findByIdAndSubjectIdAndCreatedByTeacherId(
            UUID id,
            UUID subjectId,
            UUID createdByTeacherId
    );

    Page<Exam> findAllBySubjectIdAndCreatedByTeacherId(
            UUID subjectId,
            UUID createdByTeacherId,
            Pageable pageable
    );

    Page<Exam> findAllBySubjectIdAndCreatedByTeacherIdAndStatus(
            UUID subjectId,
            UUID createdByTeacherId,
            ExamStatus status,
            Pageable pageable
    );

    boolean existsByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from Exam e
            where e.id = :id
              and e.subjectId = :subjectId
              and e.createdByTeacherId = :teacherId
            """)
    Optional<Exam> findOwnedForUpdate(
            @Param("id") UUID id,
            @Param("subjectId") UUID subjectId,
            @Param("teacherId") UUID teacherId
    );
}
