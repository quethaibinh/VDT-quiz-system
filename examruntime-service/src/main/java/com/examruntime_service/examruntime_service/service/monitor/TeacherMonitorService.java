package com.examruntime_service.examruntime_service.service.monitor;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.RuntimeActivationDTO;
import com.examruntime_service.examruntime_service.model.dto.monitor.MonitorSnapshotDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.service.activation.RuntimeActivationCache;
import com.examruntime_service.examruntime_service.util.exception.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
/**
 * Service doc snapshot monitor cho teacher va kiem tra quyen subscribe topic.
 *
 * Runtime khong giu bang exam owner rieng. OwnerTeacherId den tu activation metadata
 * trong Redis, fallback sang Exam Service khi cache bi mat.
 */
public class TeacherMonitorService {

    private final RuntimeActivationCache activationCache;
    private final ExamServiceSnapshotClient snapshotClient;
    private final MonitorStateService monitorStateService;

    public TeacherMonitorService(
            RuntimeActivationCache activationCache,
            ExamServiceSnapshotClient snapshotClient,
            MonitorStateService monitorStateService
    ) {
        this.activationCache = activationCache;
        this.snapshotClient = snapshotClient;
        this.monitorStateService = monitorStateService;
    }

    public MonitorSnapshotDTO snapshot(UUID examId, UUID teacherId) {
        // REST snapshot va WebSocket subscribe phai dung chung logic authorize
        // de khong co duong nao doc duoc exam cua teacher khac.
        RuntimeActivationMetadata metadata = activationCache.getActivation(examId);
        UUID ownerTeacherId = metadata != null ? metadata.ownerTeacherId() : null;
        if (ownerTeacherId == null) {
            ownerTeacherId = fallbackOwnerTeacherId(examId);
        }
        if (ownerTeacherId == null) {
            throw new NotFoundException("EXAM_MONITOR_METADATA_NOT_FOUND");
        }
        if (!ownerTeacherId.equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "TEACHER_NOT_EXAM_OWNER");
        }
        return monitorStateService.readSnapshot(examId);
    }

    public boolean canMonitor(UUID examId, UUID teacherId) {
        try {
            // Ham nay duoc goi trong STOMP interceptor, nen tra false thay vi nem exception
            // de interceptor co the reject frame gon gang.
            RuntimeActivationMetadata metadata = activationCache.getActivation(examId);
            UUID ownerTeacherId = metadata != null ? metadata.ownerTeacherId() : null;
            if (ownerTeacherId == null) {
                ownerTeacherId = fallbackOwnerTeacherId(examId);
            }
            return ownerTeacherId != null && ownerTeacherId.equals(teacherId);
        } catch (Exception exception) {
            return false;
        }
    }

    private UUID fallbackOwnerTeacherId(UUID examId) {
        // Fallback nay van la internal API, khong mo quyen cho client truc tiep.
        RuntimeActivationDTO dto = snapshotClient.getRuntimeActivation(examId);
        return dto != null ? dto.ownerTeacherId() : null;
    }
}
