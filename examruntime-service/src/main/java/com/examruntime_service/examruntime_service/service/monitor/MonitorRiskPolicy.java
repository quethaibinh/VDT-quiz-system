package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringEventType;
import com.examruntime_service.examruntime_service.model.entity.enums.ProctoringSeverity;
import com.examruntime_service.examruntime_service.model.entity.enums.RiskLevel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
/**
 * Chinh sach tinh diem rui ro MVP.
 *
 * Cach tinh co chu dich don gian:
 * - ONLINE/OFFLINE/RETURNED khong cong loi.
 * - Cac hanh vi roi khoi man hinh thi cong diem trung binh.
 * - Tin hieu nghiem trong nhu multi-instance/ip change cong diem cao.
 * - Lock khi tong vi pham hoac score vuot nguong cau hinh.
 */
public class MonitorRiskPolicy {

    private final int maxViolationAllowed;

    public MonitorRiskPolicy(
            @Value("${examruntime.monitor.max-violation-allowed:5}") int maxViolationAllowed
    ) {
        this.maxViolationAllowed = maxViolationAllowed;
    }

    public ProctoringSeverity severity(ProctoringEventType type) {
        // Severity phuc vu UI/audit; risk weight phuc vu quyet dinh lock.
        return switch (type) {
            case ONLINE, OFFLINE, RETURNED -> ProctoringSeverity.INFO;
            case MULTI_INSTANCE_DETECTED, IP_CHANGED, USER_AGENT_CHANGED, CLIENT_TIME_DRIFT,
                    ANSWER_BURST_SUSPECTED, LOCKED -> ProctoringSeverity.HIGH;
            default -> ProctoringSeverity.MEDIUM;
        };
    }

    public BigDecimal weight(ProctoringEventType type) {
        // Dung BigDecimal de sau nay co the chinh trong so le ma khong doi contract.
        return switch (type) {
            case ONLINE, OFFLINE, RETURNED -> BigDecimal.ZERO;
            case COPY_ATTEMPT, PASTE_ATTEMPT, CONTEXT_MENU_OPENED -> BigDecimal.valueOf(0.75);
            case MULTI_INSTANCE_DETECTED, IP_CHANGED, USER_AGENT_CHANGED, CLIENT_TIME_DRIFT,
                    ANSWER_BURST_SUSPECTED -> BigDecimal.valueOf(2);
            case LOCKED -> BigDecimal.ZERO;
            default -> BigDecimal.ONE;
        };
    }

    public RiskLevel level(BigDecimal score, int totalViolationCount) {
        return level(score, totalViolationCount, this.maxViolationAllowed);
    }

    public RiskLevel level(BigDecimal score, int totalViolationCount, int maxViolation) {
        // Risk level cho dashboard; lock decision van nam rieng trong shouldLock.
        if (totalViolationCount >= maxViolation || score.compareTo(BigDecimal.valueOf(maxViolation)) >= 0) {
            return RiskLevel.CRITICAL;
        }
        if (score.compareTo(BigDecimal.valueOf(3)) >= 0) {
            return RiskLevel.HIGH;
        }
        if (score.compareTo(BigDecimal.ONE) >= 0) {
            return RiskLevel.MEDIUM;
        }
        return RiskLevel.LOW;
    }

    public boolean shouldLock(BigDecimal score, int totalViolationCount) {
        return shouldLock(score, totalViolationCount, this.maxViolationAllowed, "LOCK");
    }

    public boolean shouldLock(BigDecimal score, int totalViolationCount, int maxViolation, String handleViolation) {
        if (!"LOCK".equalsIgnoreCase(handleViolation)) {
            return false;
        }
        // Dieu kien lock dung ca count lan score de ho tro ca luat don gian va luat co trong so.
        return totalViolationCount >= maxViolation
                || score.compareTo(BigDecimal.valueOf(maxViolation)) >= 0;
    }
}
