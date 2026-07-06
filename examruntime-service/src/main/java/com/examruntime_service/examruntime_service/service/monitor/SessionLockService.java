package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.SessionMonitorState;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import org.springframework.stereotype.Service;

@Service
public class SessionLockService {

    public boolean lock(ExamSession session, SessionMonitorState state, String reason) {
        if (session.getStatus() == ExamSessionStatus.LOCKED) {
            return false;
        }
        session.setStatus(ExamSessionStatus.LOCKED);
        session.setLockedReason(reason);
        state.setLocked(true);
        state.setLockedReason(reason);
        return true;
    }
}
