package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizRoomDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizTeacherSnapshotDTO;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizTeacherSnapshotService;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizRoomService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Teacher API dieu khien vong doi room live quiz.
 *
 * Student join/play dung API rieng de khong tron
 * quyen giao vien voi quyen hoc sinh.
 */
@RestController
@RequestMapping("/v1/api/examruntime-service/teacher/live-quizzes")
public class TeacherLiveQuizRoomController {

    private final LiveQuizRoomService roomService;
    private final LiveQuizTeacherSnapshotService snapshotService;

    /**
     * Inject service dieu khien room va service doc snapshot teacher.
     */
    public TeacherLiveQuizRoomController(
            LiveQuizRoomService roomService,
            LiveQuizTeacherSnapshotService snapshotService
    ) {
        this.roomService = roomService;
        this.snapshotService = snapshotService;
    }

    /**
     * Lay thong tin room neu teacher la owner.
     */
    @GetMapping("/{roomId}")
    public LiveQuizRoomDTO get(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return roomService.get(roomId, teacherId(principal));
    }

    /**
     * Mo room sang lobby OPEN de student co the join.
     */
    @PostMapping("/{roomId}/open")
    /*
     * Open la buoc rieng sau prepare. Nhu vay giao vien co the tao ma phong truoc,
     * hien len man hinh, roi moi cho hoc sinh vao khi san sang.
     */
    public LiveQuizRoomDTO open(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return roomService.open(roomId, teacherId(principal));
    }

    /**
     * Dong room, chan join/play tiep theo.
     */
    @PostMapping("/{roomId}/close")
    public LiveQuizRoomDTO close(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return roomService.close(roomId, teacherId(principal));
    }

    /**
     * Bat dau lam bai: chuyen OPEN sang STARTED va khoa join moi.
     */
    @PostMapping("/{roomId}/start")
    public LiveQuizRoomDTO start(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return roomService.start(roomId, teacherId(principal));
    }

    /**
     * Lay snapshot cho man teacher gom participant, summary va leaderboard.
     */
    @GetMapping("/{roomId}/snapshot")
    public LiveQuizTeacherSnapshotDTO snapshot(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return snapshotService.snapshot(roomId, teacherId(principal));
    }

    /**
     * Chuyen principal dang String userId sang UUID teacher.
     */
    private UUID teacherId(ExamRuntimeUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
