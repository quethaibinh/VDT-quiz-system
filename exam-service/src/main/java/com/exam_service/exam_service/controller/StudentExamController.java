package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamUserPrincipal;
import com.exam_service.exam_service.model.dto.exams.StudentExamDetailDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamPageDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamSummaryDTO;
import com.exam_service.exam_service.service.exams.StudentExamReadService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/exam-service/student/exams")
/**
 * Cung cap API cho phep hoc sinh tim kiem ca thi va xem chi tiet ca thi duoc gan.
 */
public class StudentExamController {

    private final StudentExamReadService studentExamReadService;

    public StudentExamController(StudentExamReadService studentExamReadService) {
        this.studentExamReadService = studentExamReadService;
    }

    @GetMapping
    /**
     * Lay danh sach ca thi duoc phan cong cho hoc sinh hien tai.
     */
    public StudentExamPageDTO list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return studentExamReadService.list(studentId(principal), status, page, size);
    }

    @GetMapping("/{examId}")
    /**
     * Lay thong tin chi tiet cua mot ca thi duoc phan cong.
     */
    public StudentExamDetailDTO get(
            @PathVariable UUID examId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return studentExamReadService.get(examId, studentId(principal));
    }

    private UUID studentId(ExamUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
