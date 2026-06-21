package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamUserPrincipal;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.model.dto.exams.ExamDetailDTO;
import com.exam_service.exam_service.model.dto.exams.ExamDraftRequestDTO;
import com.exam_service.exam_service.model.dto.exams.ExamSummaryDTO;
import com.exam_service.exam_service.service.exams.TeacherExamDraftService;
import com.exam_service.exam_service.service.exams.TeacherExamSchedulingService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/exam-service/teacher/subjects/{subjectId}/exams")
/**
 * Cung cap API de giao vien quan ly ca thi trong pham vi mot mon hoc.
 * Teacher ID luon lay tu principal, khong nhan tu request body.
 */
public class TeacherExamController {

    private final TeacherExamDraftService examService;
    private final TeacherExamSchedulingService schedulingService;

    public TeacherExamController(
            TeacherExamDraftService examService,
            TeacherExamSchedulingService schedulingService
    ) {
        this.examService = examService;
        this.schedulingService = schedulingService;
    }

    @PostMapping
    /**
     * Tao ca thi moi o trang thai DRAFT tu cau hinh va bo cau hoi da chon.
     */
    public ExamDetailDTO create(
            @PathVariable UUID subjectId,
            @Valid @RequestBody ExamDraftRequestDTO request,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return examService.create(subjectId, teacherId(principal), request);
    }

    @GetMapping
    /**
     * Lay danh sach ca thi cua chinh giao vien, co loc va phan trang.
     */
    public PageResponseDTO<ExamSummaryDTO> list(
            @PathVariable UUID subjectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "updatedAt,desc") String sort,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return examService.list(
                subjectId,
                teacherId(principal),
                status,
                keyword,
                page,
                size,
                sort
        );
    }

    @GetMapping("/{examId}")
    /**
     * Lay chi tiet ca thi neu ca thi thuoc mon hoc va giao vien hien tai.
     */
    public ExamDetailDTO get(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return examService.get(subjectId, examId, teacherId(principal));
    }

    @PutMapping("/{examId}")
    /**
     * Thay the cau hinh cua ca thi DRAFT; khong cho doi owner hoac subject.
     */
    public ExamDetailDTO update(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @Valid @RequestBody ExamDraftRequestDTO request,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return examService.update(subjectId, examId, teacherId(principal), request);
    }

    @PatchMapping("/{examId}/cancel")
    /**
     * Huy ca thi bang chuyen trang thai, khong xoa vat ly du lieu.
     */
    public ExamDetailDTO cancel(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return examService.cancel(subjectId, examId, teacherId(principal));
    }

    @PatchMapping("/{examId}/schedule")
    /**
     * Dong bang candidate pool va chuyen ca thi sang SCHEDULED.
     * Redis duoc cap nhat bat dong bo qua transactional outbox.
     */
    public ExamDetailDTO schedule(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return schedulingService.schedule(subjectId, examId, teacherId(principal));
    }

    private UUID teacherId(ExamUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
