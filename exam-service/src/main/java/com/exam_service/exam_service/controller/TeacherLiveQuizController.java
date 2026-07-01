package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamUserPrincipal;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizDetailDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizPrepareResponseDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizRequestDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizSummaryDTO;
import com.exam_service.exam_service.service.livequiz.LiveQuizPrepareService;
import com.exam_service.exam_service.service.livequiz.TeacherLiveQuizService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/exam-service/teacher/subjects/{subjectId}/live-quizzes")
/**
 * API rieng cho giao vien cau hinh live quiz.
 *
 * Cac endpoint nay co route va service rieng de khong dung vao flow tao ca thi exam
 * co lich. Live quiz van tai su dung bang Exam, nhung luon bi gioi han boi
 * examType = LIVE_QUIZ o tang service.
 */
public class TeacherLiveQuizController {

    private final TeacherLiveQuizService liveQuizService;
    private final LiveQuizPrepareService prepareService;

    public TeacherLiveQuizController(
            TeacherLiveQuizService liveQuizService,
            LiveQuizPrepareService prepareService
    ) {
        this.liveQuizService = liveQuizService;
        this.prepareService = prepareService;
    }

    @PostMapping
    public LiveQuizDetailDTO create(
            @PathVariable UUID subjectId,
            @Valid @RequestBody LiveQuizRequestDTO request,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return liveQuizService.create(subjectId, teacherId(principal), request);
    }

    @GetMapping
    public PageResponseDTO<LiveQuizSummaryDTO> list(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return liveQuizService.list(subjectId, teacherId(principal), status, keyword, page, size, sort);
    }

    @GetMapping("/{quizId}")
    public LiveQuizDetailDTO get(
            @PathVariable UUID subjectId,
            @PathVariable UUID quizId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return liveQuizService.get(subjectId, quizId, teacherId(principal));
    }

    @PutMapping("/{quizId}")
    public LiveQuizDetailDTO update(
            @PathVariable UUID subjectId,
            @PathVariable UUID quizId,
            @Valid @RequestBody LiveQuizRequestDTO request,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return liveQuizService.update(subjectId, quizId, teacherId(principal), request);
    }

    @PostMapping("/{quizId}/prepare")
    /*
     * Prepare chi dong bang snapshot cau hoi va tao room o Runtime voi status PREPARING.
     * Giao vien phai goi API open rieng ben Runtime thi hoc sinh moi co the vao phong.
     */
    public LiveQuizPrepareResponseDTO prepare(
            @PathVariable UUID subjectId,
            @PathVariable UUID quizId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return prepareService.prepare(subjectId, quizId, teacherId(principal));
    }

    private UUID teacherId(ExamUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
