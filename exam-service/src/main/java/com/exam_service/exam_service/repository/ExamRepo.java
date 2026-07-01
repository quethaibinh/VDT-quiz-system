package com.exam_service.exam_service.repository;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

import java.time.OffsetDateTime;
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

    Page<Exam> findAllBySubjectIdAndCreatedByTeacherIdAndExamType(
            UUID subjectId,
            UUID createdByTeacherId,
            ExamType examType,
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

    @Query("""
            select e.id from Exam e
            where e.examType = com.exam_service.exam_service.model.entity.enums.ExamType.STANDARD_EXAM
              and e.status = :status
              and e.startAt <= :windowEnd
            """)
    Page<UUID> findCandidateIdsForActivation(
            @Param("status") ExamStatus status,
            @Param("windowEnd") OffsetDateTime windowEnd,
            Pageable pageable
    );

    @Query("""
            select e.id from Exam e
            where e.examType = com.exam_service.exam_service.model.entity.enums.ExamType.STANDARD_EXAM
              and e.status = com.exam_service.exam_service.model.entity.enums.ExamStatus.ACTIVE
              and e.endAt is not null
              and e.endAt <= :closeBefore
            order by e.endAt asc
            """)
    Page<UUID> findCandidateIdsForClosing(
            @Param("closeBefore") OffsetDateTime closeBefore,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Exam e where e.id = :id")
    Optional<Exam> findByIdForUpdate(@Param("id") UUID id);

    // Tim kiem ca thi duoc phan cong cho hoc sinh voi bo loc availability
    @Query("""
        select e from Exam e
        where e.id in (
            select a.examId from ExamAssignment a
            where a.studentId = :studentId
              and a.status = com.exam_service.exam_service.model.entity.enums.AssignmentStatus.ASSIGNED
        )
        and e.examType = com.exam_service.exam_service.model.entity.enums.ExamType.STANDARD_EXAM
        and e.status in (
            com.exam_service.exam_service.model.entity.enums.ExamStatus.SCHEDULED,
            com.exam_service.exam_service.model.entity.enums.ExamStatus.ACTIVE,
            com.exam_service.exam_service.model.entity.enums.ExamStatus.CLOSED
        )
        and (
            :availability is null or
            (:availability = 'UPCOMING' and e.startAt > :now) or
            (:availability = 'OPEN' and e.startAt <= :now and e.endAt >= :now) or
            (:availability = 'ENDED' and (e.endAt < :now or e.status = com.exam_service.exam_service.model.entity.enums.ExamStatus.CLOSED))
        )
    """)
    Page<Exam> findAssignedExamsForStudent(
            @Param("studentId") UUID studentId,
            @Param("availability") String availability,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );

    // Lay chi tiet ca thi duoc phan cong cho hoc sinh
    @Query("""
        select e from Exam e
        where e.id = :examId
          and e.examType = com.exam_service.exam_service.model.entity.enums.ExamType.STANDARD_EXAM
          and e.id in (
              select a.examId from ExamAssignment a
              where a.studentId = :studentId
                and a.status = com.exam_service.exam_service.model.entity.enums.AssignmentStatus.ASSIGNED
          )
          and e.status in (
              com.exam_service.exam_service.model.entity.enums.ExamStatus.SCHEDULED,
              com.exam_service.exam_service.model.entity.enums.ExamStatus.ACTIVE,
              com.exam_service.exam_service.model.entity.enums.ExamStatus.CLOSED
          )
    """)
    Optional<Exam> findAssignedExamForStudent(
            @Param("examId") UUID examId,
            @Param("studentId") UUID studentId
    );
}

