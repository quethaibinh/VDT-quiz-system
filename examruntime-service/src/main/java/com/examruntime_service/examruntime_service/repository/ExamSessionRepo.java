package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExamSessionRepo extends JpaRepository<ExamSession, UUID> {

    Optional<ExamSession> findByExamIdAndStudentIdAndAttemptNo(UUID examId, UUID studentId, int attemptNo);

    List<ExamSession> findAllByExamIdAndStatusIn(UUID examId, Collection<ExamSessionStatus> statuses);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ExamSession s where s.id = :id")
    Optional<ExamSession> findByIdForUpdate(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s from ExamSession s
            where s.examId = :examId
              and s.studentId = :studentId
              and s.attemptNo = :attemptNo
            """)
    Optional<ExamSession> findByExamStudentAttemptForUpdate(
            @Param("examId") UUID examId,
            @Param("studentId") UUID studentId,
            @Param("attemptNo") int attemptNo
    );
}
