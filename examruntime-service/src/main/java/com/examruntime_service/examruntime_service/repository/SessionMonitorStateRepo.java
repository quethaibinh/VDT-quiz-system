package com.examruntime_service.examruntime_service.repository;

import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.MonitorOnlineStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SessionMonitorStateRepo extends JpaRepository<SessionMonitorState, UUID> {

    List<SessionMonitorState> findAllByExamId(UUID examId);

    List<SessionMonitorState> findAllByExamIdAndOnlineStatus(UUID examId, MonitorOnlineStatus onlineStatus);

    List<SessionMonitorState> findAllByExamIdAndRiskLevel(UUID examId, RiskLevel riskLevel);
}
