package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizAnswer;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.OutboxEvent;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizAnswerRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantPaperRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.repository.OutboxEventRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveQuizCloseFinalizationServiceTest {

    private final UUID roomId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID participantId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionId = UUID.randomUUID();

    private LiveQuizRoomRepo roomRepo;
    private LiveQuizParticipantRepo participantRepo;
    private LiveQuizParticipantPaperRepo paperRepo;
    private LiveQuizAnswerRepo answerRepo;
    private OutboxEventRepo outboxEventRepo;
    private LiveQuizRoomCache roomCache;
    private LiveQuizRealtimePublisher realtimePublisher;
    private LiveQuizCloseFinalizationService service;

    @BeforeEach
    void setUp() {
        roomRepo = mock(LiveQuizRoomRepo.class);
        participantRepo = mock(LiveQuizParticipantRepo.class);
        paperRepo = mock(LiveQuizParticipantPaperRepo.class);
        answerRepo = mock(LiveQuizAnswerRepo.class);
        outboxEventRepo = mock(OutboxEventRepo.class);
        roomCache = mock(LiveQuizRoomCache.class);
        realtimePublisher = mock(LiveQuizRealtimePublisher.class);
        service = new LiveQuizCloseFinalizationService(
                roomRepo,
                participantRepo,
                paperRepo,
                answerRepo,
                outboxEventRepo,
                roomCache,
                realtimePublisher,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-07T08:00:00Z"), ZoneOffset.UTC)
        );

        when(roomRepo.findByIdForUpdate(roomId)).thenReturn(Optional.of(room()));
        when(roomCache.getPaperPool(roomId)).thenReturn(paperPool());
        when(roomCache.getAnswerKey(roomId)).thenReturn(answerKey());
        when(participantRepo.findByRoomIdForUpdate(roomId)).thenReturn(List.of(participant()));
        when(paperRepo.findByParticipantId(participantId)).thenReturn(Optional.of(participantPaper()));
        when(answerRepo.findByRoomIdAndParticipantIdOrderByQuestionPositionAsc(roomId, participantId)).thenReturn(List.of());
        when(answerRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(outboxEventRepo.findFirstByAggregateTypeAndAggregateIdAndEventType(any(), any(), any())).thenReturn(Optional.empty());
        when(outboxEventRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void closeFinalizationLocksParticipantsBeforeRecalculatingFinalScores() {
        service.closeAndFinalize(roomId);

        verify(participantRepo).findByRoomIdForUpdate(roomId);
        verify(participantRepo, never()).findByRoomId(roomId);
        verify(participantRepo).saveAll(any());
        verify(outboxEventRepo).save(any(OutboxEvent.class));
        verify(realtimePublisher).publish(any(), any());
    }

    private LiveQuizRoom room() {
        LiveQuizRoom room = new LiveQuizRoom();
        room.setId(roomId);
        room.setExamId(examId);
        room.setOwnerTeacherId(UUID.randomUUID());
        room.setRoomCode("QZ0001");
        room.setQuizTitle("Live Quiz");
        room.setSubjectName("Math");
        room.setQuestionCount(1);
        room.setShowLeaderboard(true);
        room.setStatus(LiveQuizRoomStatus.STARTED);
        return room;
    }

    private LiveQuizParticipant participant() {
        LiveQuizParticipant participant = new LiveQuizParticipant();
        participant.setId(participantId);
        participant.setRoomId(roomId);
        participant.setStudentId(studentId);
        participant.setStudentNameSnapshot("Student Demo");
        participant.setStatus(LiveQuizParticipantStatus.IN_PROGRESS);
        participant.setCurrentQuestionId(questionId);
        participant.setCurrentQuestionPosition(0);
        participant.setCurrentQuestionStartedAt(OffsetDateTime.parse("2026-07-07T07:59:30Z"));
        participant.setCurrentQuestionEndsAt(OffsetDateTime.parse("2026-07-07T08:00:30Z"));
        participant.setTotalQuestions(1);
        participant.setTotalScore(BigDecimal.ZERO);
        participant.setMaxScore(BigDecimal.TEN);
        return participant;
    }

    private LiveQuizParticipantPaper participantPaper() {
        LiveQuizParticipantPaper paper = new LiveQuizParticipantPaper();
        paper.setId(UUID.randomUUID());
        paper.setRoomId(roomId);
        paper.setParticipantId(participantId);
        paper.setStudentId(studentId);
        paper.setQuestionOrder("[\"" + questionId + "\"]");
        return paper;
    }

    private ExamPaperPoolDTO paperPool() {
        return new ExamPaperPoolDTO(
                examId,
                1,
                1,
                0,
                0,
                List.of(new PaperQuestionDTO(
                        questionId,
                        1,
                        "EASY",
                        "SINGLE_CHOICE",
                        "Question?",
                        "TEXT",
                        10,
                        30,
                        List.of(new PaperOptionDTO(optionId, "A", "A", "TEXT"))
                ))
        );
    }

    private ExamAnswerKeyDTO answerKey() {
        return new ExamAnswerKeyDTO(
                examId,
                1,
                List.of(new AnswerEntryDTO(questionId, List.of(optionId), 10))
        );
    }
}
