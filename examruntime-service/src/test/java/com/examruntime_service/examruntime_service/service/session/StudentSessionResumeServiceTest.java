package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.session.StudentAnswerDTO;
import com.examruntime_service.examruntime_service.model.entity.ExamSession;
import com.examruntime_service.examruntime_service.model.entity.enums.ExamSessionStatus;
import com.examruntime_service.examruntime_service.repository.ExamSessionRepo;
import com.examruntime_service.examruntime_service.service.paper.RuntimePaperPoolLoader;
import com.examruntime_service.examruntime_service.service.paper.StudentPaperMapper;
import com.examruntime_service.examruntime_service.service.session.resume.AnswerSnapshotReader;
import com.examruntime_service.examruntime_service.service.session.resume.StudentSessionResumeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentSessionResumeServiceTest {

    private ExamSessionRepo examSessionRepo;
    private AnswerSnapshotReader answerSnapshotReader;
    private RuntimePaperPoolLoader paperPoolLoader;
    private StudentPaperMapper paperMapper;
    private Clock clock;
    private StudentSessionResumeService resumeService;

    private final UUID sessionId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final Instant fixedInstant = Instant.parse("2026-07-01T08:00:00Z");

    @BeforeEach
    void setUp() {
        examSessionRepo = mock(ExamSessionRepo.class);
        answerSnapshotReader = mock(AnswerSnapshotReader.class);
        paperPoolLoader = mock(RuntimePaperPoolLoader.class);
        paperMapper = mock(StudentPaperMapper.class);
        clock = Clock.fixed(fixedInstant, ZoneId.of("UTC"));

        resumeService = new StudentSessionResumeService(
                examSessionRepo,
                answerSnapshotReader,
                paperPoolLoader,
                paperMapper,
                new ObjectMapper(),
                clock
        );
    }

    @Test
    void resumeCreatedSessionSuccess() {
        ExamSession session = createdSession();
        when(examSessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

        var response = resumeService.resumeSession(sessionId, studentId);

        assertThat(response.getStatus()).isEqualTo(ExamSessionStatus.CREATED);
        assertThat(response.getQuestions()).isEmpty();
        assertThat(response.getAnswers()).isEmpty();
    }

    @Test
    void resumeInProgressUsesSnapshotReader() {
        ExamSession session = inProgressSession();
        when(examSessionRepo.findById(sessionId)).thenReturn(Optional.of(session));
        when(paperPoolLoader.load(any(), anyInt(), any()))
                .thenReturn(new ExamPaperPoolDTO(examId, 1, 0, 0, 0, Collections.emptyList()));
        when(answerSnapshotReader.readForResume(eq(session), any()))
                .thenReturn(List.of(StudentAnswerDTO.builder()
                        .questionId(questionId)
                        .selectedOptionIds(List.of())
                        .answerText(null)
                        .markedForReview(false)
                        .build()));

        var response = resumeService.resumeSession(sessionId, studentId);

        assertThat(response.getStatus()).isEqualTo(ExamSessionStatus.IN_PROGRESS);
        assertThat(response.getAnswers()).hasSize(1);
        verify(answerSnapshotReader).readForResume(eq(session), any());
    }

    @Test
    void resumeCreatedSessionDoesNotLoadPaperPool() {
        ExamSession session = createdSession();
        when(examSessionRepo.findById(sessionId)).thenReturn(Optional.of(session));

        resumeService.resumeSession(sessionId, studentId);

        verify(paperPoolLoader, never()).load(any(), anyInt(), any());
        verify(answerSnapshotReader, never()).readForResume(any(), any());
    }

    private ExamSession createdSession() {
        ExamSession session = new ExamSession();
        session.setId(sessionId);
        session.setExamId(examId);
        session.setStudentId(studentId);
        session.setStatus(ExamSessionStatus.CREATED);
        return session;
    }

    private ExamSession inProgressSession() {
        ExamSession session = createdSession();
        session.setStatus(ExamSessionStatus.IN_PROGRESS);
        session.setQuestionOrder("[]");
        session.setOptionOrders("{}");
        return session;
    }
}
