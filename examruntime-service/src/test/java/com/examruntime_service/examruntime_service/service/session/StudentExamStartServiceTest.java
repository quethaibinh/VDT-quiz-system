package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.runtime.RuntimeActivationMetadata;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.repository.SessionAnswerRepo;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperGenerator;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentExamStartServiceTest {

    private ExamSessionRepo examSessionRepo;
    private SessionAnswerRepo sessionAnswerRepo;
    private RuntimeActivationResolver activationResolver;
    private RuntimePaperPoolLoader paperPoolLoader;
    private StudentPaperGenerator paperGenerator;
    private StudentPaperMapper paperMapper;
    private Clock clock;
    private StudentExamStartService startService;

    private final UUID examId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        sessionAnswerRepo = mock(SessionAnswerRepo.class);
        activationResolver = mock(RuntimeActivationResolver.class);
        paperPoolLoader = mock(RuntimePaperPoolLoader.class);
        paperGenerator = mock(StudentPaperGenerator.class);
        paperMapper = mock(StudentPaperMapper.class);
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        startService = new StudentExamStartService(
                examSessionRepo,
                sessionAnswerRepo,
                activationResolver,
                paperPoolLoader,
                paperGenerator,
                paperMapper,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void testStartExamSuccess() {
        ExamSession session = new ExamSession();
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.CREATED);

        when(examSessionRepo.findByExamStudentAttemptForUpdate(examId, studentId, 1))
                .thenReturn(Optional.of(session));

        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );
        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);

        ExamPaperPoolDTO pool = new ExamPaperPoolDTO(examId, 1, 0, 0, 0, Collections.emptyList());
        when(paperPoolLoader.load(any(), anyInt(), any())).thenReturn(pool);

        when(paperGenerator.generateSeed(any(), any(), anyInt())).thenReturn(12345L);
        when(paperGenerator.generatePaperStructure(any(), any(Long.class)))
                .thenReturn(new StudentPaperGenerator.PaperStructure(Collections.emptyList(), Collections.emptyMap()));

        var response = startService.startExam(examId, studentId);

        assertThat(response.getStatus()).isEqualTo(ExamSessionStatus.IN_PROGRESS);
        verify(examSessionRepo).save(session);
    }

    @Test
    void testStartExamTooEarlyThrows() {
        ExamSession session = new ExamSession();
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.CREATED);

        when(examSessionRepo.findByExamStudentAttemptForUpdate(examId, studentId, 1))
                .thenReturn(Optional.of(session));

        // startAt la 5 phut nua nhung now la hien tai -> chua den gio bat dau
        RuntimeActivationMetadata metadata = new RuntimeActivationMetadata(
                examId,
                1,
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(300), ZoneOffset.UTC),
                OffsetDateTime.ofInstant(fixedInstant.plusSeconds(3600), ZoneOffset.UTC),
                10,
                10,
                "READY",
                OffsetDateTime.ofInstant(fixedInstant, ZoneOffset.UTC),
                "REDIS"
        );
        when(activationResolver.getReadyActivation(examId)).thenReturn(metadata);

        assertThatThrownBy(() -> startService.startExam(examId, studentId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_NOT_STARTED");
    }
}
