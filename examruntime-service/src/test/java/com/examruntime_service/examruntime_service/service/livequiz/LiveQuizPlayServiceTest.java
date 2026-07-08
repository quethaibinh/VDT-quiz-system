package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.client.QuestionMediaClient;
import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizAnswerRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizLeaderboardEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizSelectedOptionResult;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizAnswer;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipant;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizParticipantPaper;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizParticipantStatus;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizAnswerRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantPaperRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizParticipantRepo;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LiveQuizPlayServiceTest {

    private final UUID roomId = UUID.randomUUID();
    private final UUID examId = UUID.randomUUID();
    private final UUID studentId = UUID.randomUUID();
    private final UUID participantId = UUID.randomUUID();
    private final UUID questionId = UUID.randomUUID();
    private final UUID optionAId = UUID.randomUUID();
    private final UUID optionBId = UUID.randomUUID();
    private final UUID optionCId = UUID.randomUUID();
    private final OffsetDateTime now = OffsetDateTime.parse("2026-07-03T10:00:05Z");

    private LiveQuizRoomRepo roomRepo;
    private LiveQuizParticipantRepo participantRepo;
    private LiveQuizParticipantPaperRepo paperRepo;
    private LiveQuizAnswerRepo answerRepo;
    private LiveQuizRoomCache roomCache;
    private LiveQuizRealtimePublisher realtimePublisher;
    private LiveQuizTeacherSnapshotService snapshotService;
    private QuestionMediaClient questionMediaClient;
    private LiveQuizPlayService service;

    @BeforeEach
    void setUp() {
        roomRepo = mock(LiveQuizRoomRepo.class);
        participantRepo = mock(LiveQuizParticipantRepo.class);
        paperRepo = mock(LiveQuizParticipantPaperRepo.class);
        answerRepo = mock(LiveQuizAnswerRepo.class);
        roomCache = mock(LiveQuizRoomCache.class);
        realtimePublisher = mock(LiveQuizRealtimePublisher.class);
        snapshotService = mock(LiveQuizTeacherSnapshotService.class);
        questionMediaClient = mock(QuestionMediaClient.class);
        service = new LiveQuizPlayService(
                roomRepo,
                participantRepo,
                paperRepo,
                answerRepo,
                roomCache,
                realtimePublisher,
                snapshotService,
                new LiveQuizScoringPolicy(),
                questionMediaClient,
                new ObjectMapper(),
                Clock.fixed(Instant.parse("2026-07-03T10:00:05Z"), ZoneOffset.UTC)
        );

        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room()));
        when(paperRepo.findByParticipantId(participantId)).thenReturn(Optional.of(participantPaper()));
        when(roomCache.getPaperPool(roomId)).thenReturn(paperPool());
        when(roomCache.getAnswerKey(roomId)).thenReturn(answerKey());
        when(participantRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepo.findByRoomId(roomId)).thenReturn(List.of(participant(now.minusSeconds(5), now.plusSeconds(5))));
        when(snapshotService.currentRank(roomId, participantId)).thenReturn(1);
        when(snapshotService.leaderboard(any())).thenReturn(List.of(leaderboardEntry()));
        when(questionMediaClient.signedUrls(any())).thenReturn(Map.of());
        when(answerRepo.save(any())).thenAnswer(invocation -> {
            LiveQuizAnswer answer = invocation.getArgument(0);
            answer.setId(UUID.randomUUID());
            return answer;
        });
    }

    @Test
    void correctAnswerHalfwayReceivesPartialScore() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId)));

        assertThat(response.answerStatus()).isEqualTo(LiveQuizAnswerStatus.ANSWERED);
        assertThat(response.correct()).isTrue();
        assertThat(response.scoreAwarded()).isEqualByComparingTo("7.5000");
        assertThat(response.maxScore()).isEqualByComparingTo("10.0");
        assertThat(response.responseTimeMs()).isEqualTo(5000);
        assertThat(response.scoreRatio()).isEqualByComparingTo("0.7500");
        assertThat(response.totalScore()).isEqualByComparingTo("7.5000");
        assertThat(response.selectedOptionResults()).singleElement().satisfies(result -> {
            assertThat(result.optionId()).isEqualTo(optionAId);
            assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.CORRECT);
        });
        assertThat(participant.getAnsweredCount()).isEqualTo(1);
        assertThat(participant.getCorrectCount()).isEqualTo(1);
    }

    @Test
    void wrongAnswerBeforeDeadlineReceivesZero() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionBId)));

        assertThat(response.answerStatus()).isEqualTo(LiveQuizAnswerStatus.ANSWERED);
        assertThat(response.correct()).isFalse();
        assertThat(response.scoreAwarded()).isEqualByComparingTo("0.0000");
        assertThat(response.responseTimeMs()).isEqualTo(5000);
        assertThat(response.totalScore()).isEqualByComparingTo("0.0000");
        assertThat(response.selectedOptionResults()).singleElement().satisfies(result -> {
            assertThat(result.optionId()).isEqualTo(optionBId);
            assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.WRONG);
        });
        assertThat(participant.getWrongCount()).isEqualTo(1);
    }

    @Test
    void multiChoiceCompleteSelectionMarksSelectedOptionsCorrect() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());
        when(roomCache.getAnswerKey(roomId)).thenReturn(multiAnswerKey());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId, optionBId)));

        assertThat(response.correct()).isTrue();
        assertThat(response.selectedOptionResults()).hasSize(2);
        assertThat(response.selectedOptionResults())
                .allSatisfy(result -> assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.CORRECT));
    }

    @Test
    void multiChoiceIncompleteSelectionMarksSelectedCorrectOptionPartial() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());
        when(roomCache.getAnswerKey(roomId)).thenReturn(multiAnswerKey());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId)));

        assertThat(response.correct()).isFalse();
        assertThat(response.selectedOptionResults()).singleElement().satisfies(result -> {
            assertThat(result.optionId()).isEqualTo(optionAId);
            assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.PARTIAL_CORRECT);
        });
    }

    @Test
    void multiChoiceMixedSelectionMarksCorrectPartialAndWrongRed() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());
        when(roomCache.getAnswerKey(roomId)).thenReturn(multiAnswerKey());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId, optionCId)));

        assertThat(response.correct()).isFalse();
        assertThat(response.selectedOptionResults()).hasSize(2);
        assertThat(response.selectedOptionResults())
                .anySatisfy(result -> {
                    assertThat(result.optionId()).isEqualTo(optionAId);
                    assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.PARTIAL_CORRECT);
                })
                .anySatisfy(result -> {
                    assertThat(result.optionId()).isEqualTo(optionCId);
                    assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.WRONG);
                });
    }

    @Test
    void answerAfterDeadlineRecordsTimeoutWithZeroScore() {
        LiveQuizParticipant participant = participant(now.minusSeconds(12), now.minusSeconds(2));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.empty());

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId)));

        assertThat(response.answerStatus()).isEqualTo(LiveQuizAnswerStatus.TIMEOUT);
        assertThat(response.correct()).isFalse();
        assertThat(response.scoreAwarded()).isEqualByComparingTo("0");
        assertThat(response.responseTimeMs()).isNull();
        assertThat(response.totalScore()).isEqualByComparingTo("0");
        assertThat(response.selectedOptionResults()).isEmpty();
        assertThat(participant.getTimeoutCount()).isEqualTo(1);
    }

    @Test
    void duplicateAnswerReturnsStoredScoreWithoutRecalculation() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        participant.setTotalScore(BigDecimal.valueOf(7.5));
        LiveQuizAnswer existing = new LiveQuizAnswer();
        existing.setRoomId(roomId);
        existing.setParticipantId(participantId);
        existing.setStudentId(studentId);
        existing.setQuestionId(questionId);
        existing.setQuestionPosition(1);
        existing.setAnswerStatus(LiveQuizAnswerStatus.ANSWERED);
        existing.setCorrect(true);
        existing.setScoreAwarded(BigDecimal.valueOf(7.5));
        existing.setMaxScore(BigDecimal.TEN);
        existing.setSelectedOptionIds("[\"" + optionAId + "\"]");
        existing.setResponseTimeMs(5000);
        existing.setQuestionStartedAt(now.minusSeconds(5));
        existing.setQuestionEndsAt(now.plusSeconds(5));
        existing.setServerReceivedAt(now);
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));
        when(answerRepo.findByRoomIdAndParticipantIdAndQuestionId(roomId, participantId, questionId)).thenReturn(Optional.of(existing));

        var response = service.answer(roomId, studentId, new LiveQuizAnswerRequestDTO(questionId, List.of(optionAId)));

        assertThat(response.scoreAwarded()).isEqualByComparingTo("7.5");
        assertThat(response.totalScore()).isEqualByComparingTo("7.5");
        assertThat(response.selectedOptionResults()).singleElement().satisfies(result -> {
            assertThat(result.optionId()).isEqualTo(optionAId);
            assertThat(result.result()).isEqualTo(LiveQuizSelectedOptionResult.CORRECT);
        });
        assertThat(participant.getAnsweredCount()).isZero();
    }

    @Test
    void stateIncludesLeaderboardSnapshotWhenRoomAllowsLeaderboard() {
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));

        var response = service.state(roomId, studentId);

        assertThat(response.showLeaderboard()).isTrue();
        assertThat(response.leaderboard()).singleElement().satisfies(entry -> {
            assertThat(entry.participantId()).isEqualTo(participantId);
            assertThat(entry.studentId()).isEqualTo(studentId);
        });
    }

    @Test
    void stateDoesNotIncludeLeaderboardSnapshotWhenRoomHidesLeaderboard() {
        LiveQuizRoom room = room();
        room.setShowLeaderboard(false);
        LiveQuizParticipant participant = participant(now.minusSeconds(5), now.plusSeconds(5));
        when(roomRepo.findById(roomId)).thenReturn(Optional.of(room));
        when(participantRepo.findByRoomIdAndStudentIdForUpdate(roomId, studentId)).thenReturn(Optional.of(participant));

        var response = service.state(roomId, studentId);

        assertThat(response.showLeaderboard()).isFalse();
        assertThat(response.leaderboard()).isEmpty();
    }

    private LiveQuizRoom room() {
        LiveQuizRoom room = new LiveQuizRoom();
        room.setId(roomId);
        room.setExamId(examId);
        room.setRoomCode("A7K2Q9");
        room.setOwnerTeacherId(UUID.randomUUID());
        room.setStatus(LiveQuizRoomStatus.STARTED);
        room.setQuestionCount(1);
        room.setShowLeaderboard(true);
        return room;
    }

    private LiveQuizParticipant participant(OffsetDateTime startedAt, OffsetDateTime endsAt) {
        LiveQuizParticipant participant = new LiveQuizParticipant();
        participant.setId(participantId);
        participant.setRoomId(roomId);
        participant.setStudentId(studentId);
        participant.setStudentNameSnapshot("Student Demo");
        participant.setStatus(LiveQuizParticipantStatus.IN_PROGRESS);
        participant.setCurrentQuestionId(questionId);
        participant.setCurrentQuestionPosition(0);
        participant.setCurrentQuestionStartedAt(startedAt);
        participant.setCurrentQuestionEndsAt(endsAt);
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
                        10,
                        List.of(
                                new PaperOptionDTO(optionAId, "A", "A", "TEXT"),
                                new PaperOptionDTO(optionBId, "B", "B", "TEXT"),
                                new PaperOptionDTO(optionCId, "C", "C", "TEXT")
                        )
                ))
        );
    }

    private ExamAnswerKeyDTO answerKey() {
        return new ExamAnswerKeyDTO(
                examId,
                1,
                List.of(new AnswerEntryDTO(questionId, List.of(optionAId), 10))
        );
    }

    private ExamAnswerKeyDTO multiAnswerKey() {
        return new ExamAnswerKeyDTO(
                examId,
                1,
                List.of(new AnswerEntryDTO(questionId, List.of(optionAId, optionBId), 10))
        );
    }

    private LiveQuizLeaderboardEntryDTO leaderboardEntry() {
        return new LiveQuizLeaderboardEntryDTO(
                1,
                participantId,
                studentId,
                "Student Demo",
                BigDecimal.ZERO,
                0,
                0,
                0,
                null,
                false
        );
    }
}
