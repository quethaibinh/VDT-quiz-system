package com.examruntime_service.examruntime_service.controller;

import com.examruntime_service.examruntime_service.config.security.ExamRuntimeUserPrincipal;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizAnswerRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizAnswerResponseDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.LiveQuizCurrentQuestionDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizJoinRequestDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizJoinResponseDTO;
import com.examruntime_service.examruntime_service.model.dto.livequiz.StudentLiveQuizStateDTO;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizPlayService;
import com.examruntime_service.examruntime_service.service.livequiz.LiveQuizStudentJoinService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/examruntime-service/student/live-quizzes")
public class StudentLiveQuizController {

    private final LiveQuizStudentJoinService joinService;
    private final LiveQuizPlayService playService;

    /**
     * Inject service xu ly join lobby va play live quiz cho student.
     */
    public StudentLiveQuizController(
            LiveQuizStudentJoinService joinService,
            LiveQuizPlayService playService
    ) {
        this.joinService = joinService;
        this.playService = playService;
    }

    /**
     * Cho student nhap ma phong de vao lobby live quiz.
     */
    @PostMapping("/join")
    public StudentLiveQuizJoinResponseDTO join(
            @Valid @RequestBody StudentLiveQuizJoinRequestDTO request,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return joinService.join(studentId(principal), principal.username(), request);
    }

    /**
     * Lay trang thai hien tai cua student trong room.
     */
    @GetMapping("/{roomId}/state")
    public StudentLiveQuizStateDTO state(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return playService.state(roomId, studentId(principal));
    }

    /**
     * Lay cau hoi dang active va dong thoi start timer neu chua co cau active.
     */
    @GetMapping("/{roomId}/current-question")
    public LiveQuizCurrentQuestionDTO currentQuestion(
            @PathVariable UUID roomId,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return playService.currentQuestion(roomId, studentId(principal));
    }

    /**
     * Nhan dap an cua student cho cau hoi dang active.
     */
    @PostMapping("/{roomId}/answer")
    public LiveQuizAnswerResponseDTO answer(
            @PathVariable UUID roomId,
            @Valid @RequestBody LiveQuizAnswerRequestDTO request,
            @AuthenticationPrincipal ExamRuntimeUserPrincipal principal
    ) {
        return playService.answer(roomId, studentId(principal), request);
    }

    /**
     * Chuyen principal dang String userId sang UUID dung trong runtime DB.
     */
    private UUID studentId(ExamRuntimeUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
