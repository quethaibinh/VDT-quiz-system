package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SessionMonitorStateRepo extends JpaRepository<SessionMonitorState, UUID> {

    List<SessionMonitorState> findAllByExamId(UUID examId);

    List<SessionMonitorState> findAllByExamIdAndOnlineStatus(UUID examId, MonitorOnlineStatus onlineStatus);

    List<SessionMonitorState> findAllByExamIdAndRiskLevel(UUID examId, RiskLevel riskLevel);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select state from SessionMonitorState state where state.sessionId = :sessionId")
    Optional<SessionMonitorState> findBySessionIdForUpdate(@Param("sessionId") UUID sessionId);
}
