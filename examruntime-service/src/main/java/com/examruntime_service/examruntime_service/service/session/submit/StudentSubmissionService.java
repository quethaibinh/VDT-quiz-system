package com.examruntime_service.examruntime_service.service.session.submit;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.SubmitRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.SubmitResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import com.examruntime_service.examruntime_service.service.session.autoSave.StudentAutosaveService;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
// Dieu phoi submit thu cong: flush dap an cuoi neu con han, sau do dong bang submission.
public class StudentSubmissionService {

    private final StudentAutosaveService autosaveService;
    private final SubmissionFinalizationService finalizationService;
    private final Clock clock;

    public StudentSubmissionService(
            StudentAutosaveService autosaveService,
            SubmissionFinalizationService finalizationService,
            Clock clock
    ) {
        this.autosaveService = autosaveService;
        this.finalizationService = finalizationService;
        this.clock = clock;
    }

    public SubmitResponseDTO submit(UUID sessionId, UUID studentId, SubmitRequestDTO request) {
        SubmitRequestDTO safeRequest = request != null ? request : new SubmitRequestDTO();

        // Neu request nop bai kem batch dap an cuoi, thu ghi autosave truoc khi dong bang bai lam.
        // Buoc nay chi chap nhan khi session con trong han de tranh ghi them dap an sau deadline.
        if (safeRequest.getFinalAnswers() != null
                && !safeRequest.getFinalAnswers().isEmpty()
                && finalizationService.canAcceptManualFlush(sessionId, studentId, OffsetDateTime.now(clock))) {
            try {
                autosaveService.autosave(
                        sessionId,
                        new AutosaveRequestDTO(safeRequest.getClientSeq(), safeRequest.getFinalAnswers()),
                        studentId
                );
            } catch (ConflictException exception) {
                // Neu deadline vua het giua luc check va autosave, van cho finalizer xu ly nhu TIME_UP.
                if (!"EXAM_ALREADY_ENDED".equals(exception.getMessage())) {
                    throw exception;
                }
            }
        }

        // Dong bang submission bang finalizer dung chung voi auto-submit.
        // Idempotency key giup frontend retry request nop bai ma khong tao them submission.
        return finalizationService.finalizeSubmission(
                sessionId,
                studentId,
                SubmitReason.STUDENT,
                safeRequest.getIdempotencyKey()
        );
    }
}
