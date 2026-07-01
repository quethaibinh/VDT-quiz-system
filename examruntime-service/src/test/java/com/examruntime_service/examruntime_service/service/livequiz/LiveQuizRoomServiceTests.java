package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.client.ExamServiceSnapshotClient;
import com.examruntime_service.examruntime_service.model.dto.cache.AnswerEntryDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamAnswerKeyDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.CreateLiveQuizRoomRequestDTO;
import com.examruntime_service.examruntime_service.model.entity.LiveQuizRoom;
import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizRoomStatus;
import com.examruntime_service.examruntime_service.repository.LiveQuizRoomRepo;
import com.examruntime_service.examruntime_service.util.exception.ConflictException;
import com.examruntime_service.examruntime_service.util.exception.UnauthorizedException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LiveQuizRoomServiceTests {

    private final LiveQuizRoomRepo roomRepo = mock(LiveQuizRoomRepo.class);
    private final LiveQuizRoomCodeGenerator codeGenerator = mock(LiveQuizRoomCodeGenerator.class);
    private final ExamServiceSnapshotClient snapshotClient = mock(ExamServiceSnapshotClient.class);
    private final LiveQuizRoomCache roomCache = mock(LiveQuizRoomCache.class);
    private final LiveQuizRealtimePublisher realtimePublisher = mock(LiveQuizRealtimePublisher.class);
    private final LiveQuizRoomService service = new LiveQuizRoomService(
            roomRepo,
            codeGenerator,
            snapshotClient,
            roomCache,
            realtimePublisher
    );

    @Test
    void createRoomStartsInPreparingAndIsIdempotentByExam() {
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        mockSnapshot(examId, 1);
        when(roomRepo.findFirstByExamIdAndStatusIn(any(), any())).thenReturn(Optional.empty());
        when(codeGenerator.generate()).thenReturn("A7K2Q9");
        when(roomRepo.existsByRoomCode("A7K2Q9")).thenReturn(false);
        when(roomRepo.saveAndFlush(any())).thenAnswer(invocation -> {
            LiveQuizRoom room = invocation.getArgument(0);
            room.setId(UUID.randomUUID());
            return room;
        });

        var created = service.createRoom(new CreateLiveQuizRoomRequestDTO(examId, teacherId, 1, "CODE_ONLY"));

        assertThat(created.examId()).isEqualTo(examId);
        assertThat(created.ownerTeacherId()).isEqualTo(teacherId);
        assertThat(created.roomCode()).isEqualTo("A7K2Q9");
        assertThat(created.status()).isEqualTo(LiveQuizRoomStatus.PREPARING);
        verify(roomCache).putRoomSnapshot(any(LiveQuizRoom.class), eq(1), any(), any());

        LiveQuizRoom existing = room(examId, teacherId, LiveQuizRoomStatus.PREPARING);
        when(roomRepo.findFirstByExamIdAndStatusIn(any(), any())).thenReturn(Optional.of(existing));

        var second = service.createRoom(new CreateLiveQuizRoomRequestDTO(examId, teacherId, 1, "CODE_ONLY"));

        assertThat(second.roomId()).isEqualTo(existing.getId());
        verify(roomCache, times(2)).putRoomSnapshot(any(LiveQuizRoom.class), eq(1), any(), any());
    }

    @Test
    void openAndCloseAreIdempotent() {
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        mockSnapshot(examId, 1);
        LiveQuizRoom room = room(examId, teacherId, LiveQuizRoomStatus.PREPARING);
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomCache.hasRoomSnapshot(room)).thenReturn(false, true);

        var opened = service.open(room.getId(), teacherId);
        assertThat(opened.status()).isEqualTo(LiveQuizRoomStatus.OPEN);
        assertThat(opened.openedAt()).isNotNull();
        verify(roomCache).putRoomSnapshot(eq(room), eq(1), any(), any());
        verify(roomCache).putRoomStatus(eq(room), eq(1));

        var openedAgain = service.open(room.getId(), teacherId);
        assertThat(openedAgain.status()).isEqualTo(LiveQuizRoomStatus.OPEN);

        var closed = service.close(room.getId(), teacherId);
        assertThat(closed.status()).isEqualTo(LiveQuizRoomStatus.CLOSED);
        assertThat(closed.closedAt()).isNotNull();

        var closedAgain = service.close(room.getId(), teacherId);
        assertThat(closedAgain.status()).isEqualTo(LiveQuizRoomStatus.CLOSED);
    }

    @Test
    void startRequiresOpenRoomAndIsIdempotent() {
        UUID teacherId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        mockSnapshot(examId, 1);
        LiveQuizRoom room = room(examId, teacherId, LiveQuizRoomStatus.OPEN);
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(roomRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(roomCache.hasRoomSnapshot(room)).thenReturn(true);

        var started = service.start(room.getId(), teacherId);

        assertThat(started.status()).isEqualTo(LiveQuizRoomStatus.STARTED);
        assertThat(started.startedAt()).isNotNull();
        verify(roomCache).putRoomStatus(eq(room), eq(1));
        verify(realtimePublisher).publish(eq(room.getId()), any());

        var startedAgain = service.start(room.getId(), teacherId);
        assertThat(startedAgain.status()).isEqualTo(LiveQuizRoomStatus.STARTED);
    }

    @Test
    void startRejectsPreparingRoom() {
        UUID teacherId = UUID.randomUUID();
        LiveQuizRoom room = room(UUID.randomUUID(), teacherId, LiveQuizRoomStatus.PREPARING);
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.start(room.getId(), teacherId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("LIVE_QUIZ_ROOM_NOT_STARTABLE");
    }

    @Test
    void teacherCannotControlOtherTeacherRoom() {
        LiveQuizRoom room = room(UUID.randomUUID(), UUID.randomUUID(), LiveQuizRoomStatus.PREPARING);
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> service.open(room.getId(), UUID.randomUUID()))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessage("LIVE_QUIZ_ROOM_FORBIDDEN");
    }

    @Test
    void createRejectsUnknownJoinPolicy() {
        assertThatThrownBy(() -> service.createRoom(
                new CreateLiveQuizRoomRequestDTO(UUID.randomUUID(), UUID.randomUUID(), 1, "ASSIGNED_ONLY")
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_LIVE_QUIZ_JOIN_POLICY");
    }

    private LiveQuizRoom room(UUID examId, UUID teacherId, LiveQuizRoomStatus status) {
        LiveQuizRoom room = new LiveQuizRoom();
        room.setId(UUID.randomUUID());
        room.setExamId(examId);
        room.setOwnerTeacherId(teacherId);
        room.setRoomCode("A7K2Q9");
        room.setStatus(status);
        return room;
    }

    private void mockSnapshot(UUID examId, int snapshotVersion) {
        when(snapshotClient.getPaperPool(examId))
                .thenReturn(new ExamPaperPoolDTO(examId, snapshotVersion, 1, 0, 0, List.of()));
        when(snapshotClient.getAnswerKey(examId))
                .thenReturn(new ExamAnswerKeyDTO(examId, snapshotVersion, List.<AnswerEntryDTO>of()));
    }
}
