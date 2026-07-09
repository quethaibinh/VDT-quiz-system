package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.session.AutosaveAnswerDTO;
import com.examruntime_service.examruntime_service.model.dto.session.AutosaveRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.SubmitRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.session.SubmitResponseDTO;
import com.examruntime_service.examruntime_service.model.entity.enums.SubmitReason;
import com.examruntime_service.examruntime_service.service.session.submit.SubmissionFinalizationService;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentSubmissionServiceTest {

    private StudentAutosaveService autosaveService;
    private SubmissionFinalizationService finalizationService;
    private StudentSubmissionService service;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        autosaveService = mock(StudentAutosaveService.class);
        finalizationService = mock(SubmissionFinalizationService.class);
        service = new StudentSubmissionService(
                autosaveService,
                finalizationService,
                Clock.fixed(Instant.parse("2026-07-01T08:00:00Z"), ZoneId.of("UTC"))
        );
    }

    @Test
    void flushesFinalAnswersBeforeFinalizing() {
        SubmitResponseDTO expected = SubmitResponseDTO.builder()
                .submissionId(UUID.randomUUID())
                .sessionId(sessionId)
                .status("RECEIVED")
                .submitReason(SubmitReason.STUDENT)
                .build();
        when(finalizationService.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1"))
                .thenReturn(expected);
        when(finalizationService.canAcceptManualFlush(eq(sessionId), eq(studentId), any()))
                .thenReturn(true);

        SubmitRequestDTO request = SubmitRequestDTO.builder()
                .idempotencyKey("key-1")
                .clientSeq(9)
                .finalAnswers(List.of(AutosaveAnswerDTO.builder().questionId(questionId).build()))
                .build();

        var response = service.submit(sessionId, studentId, request);

        assertThat(response).isSameAs(expected);
        verify(finalizationService).canAcceptManualFlush(eq(sessionId), eq(studentId), any());
        ArgumentCaptor<AutosaveRequestDTO> autosaveCaptor = ArgumentCaptor.forClass(AutosaveRequestDTO.class);
        verify(autosaveService).autosave(eq(sessionId), autosaveCaptor.capture(), eq(studentId));
        assertThat(autosaveCaptor.getValue().getClientSeq()).isEqualTo(9);
        assertThat(autosaveCaptor.getValue().getAnswers()).hasSize(1);
    }

    @Test
    void finalizesWhenFinalAutosaveCrossesDeadline() {
        SubmitResponseDTO expected = SubmitResponseDTO.builder()
                .submissionId(UUID.randomUUID())
                .sessionId(sessionId)
                .status("RECEIVED")
                .submitReason(SubmitReason.TIME_UP)
                .build();
        when(finalizationService.canAcceptManualFlush(eq(sessionId), eq(studentId), any()))
                .thenReturn(true);
        doThrow(new ConflictException("EXAM_ALREADY_ENDED"))
                .when(autosaveService)
                .autosave(eq(sessionId), any(AutosaveRequestDTO.class), eq(studentId));
        when(finalizationService.finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1"))
                .thenReturn(expected);

        SubmitRequestDTO request = SubmitRequestDTO.builder()
                .idempotencyKey("key-1")
                .clientSeq(10)
                .finalAnswers(List.of(AutosaveAnswerDTO.builder().questionId(questionId).build()))
                .build();

        var response = service.submit(sessionId, studentId, request);

        assertThat(response).isSameAs(expected);
        verify(finalizationService).finalizeSubmission(sessionId, studentId, SubmitReason.STUDENT, "key-1");
    }
}
