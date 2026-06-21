package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.config.security.ExamUserPrincipal;
import com.exam_service.exam_service.model.dto.assignments.AssignmentMutationResultDTO;
import com.exam_service.exam_service.model.dto.assignments.AssignmentRequestDTO;
import com.exam_service.exam_service.model.dto.assignments.AssignmentResponseDTO;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.service.assignments.ExamAssignmentService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/exam-service/teacher/subjects/{subjectId}/exams/{examId}/assignments")
/**
 * Cung cap API xem, gan va go hoc sinh khoi ca thi cua giao vien.
 */
public class TeacherExamAssignmentController {

    private final ExamAssignmentService assignmentService;

    public TeacherExamAssignmentController(ExamAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    /**
     * Lay danh sach hoc sinh dang duoc gan vao ca thi.
     */
    public PageResponseDTO<AssignmentResponseDTO> list(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return assignmentService.list(subjectId, examId, teacherId(principal), page, size);
    }

    @PostMapping
    /**
     * Gan mot nhom nho hoc sinh theo ID; thong tin snapshot do Auth Service cung cap.
     */
    public AssignmentMutationResultDTO add(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @Valid @RequestBody AssignmentRequestDTO request,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return assignmentService.add(
                subjectId,
                examId,
                teacherId(principal),
                request.studentIds()
        );
    }

    @DeleteMapping("/{studentId}")
    /**
     * Danh dau phan cong la REMOVED de giu lich su thay vi xoa dong du lieu.
     */
    public AssignmentMutationResultDTO remove(
            @PathVariable UUID subjectId,
            @PathVariable UUID examId,
            @PathVariable UUID studentId,
            @AuthenticationPrincipal ExamUserPrincipal principal
    ) {
        return assignmentService.remove(
                subjectId,
                examId,
                teacherId(principal),
                studentId
        );
    }

    private UUID teacherId(ExamUserPrincipal principal) {
        return UUID.fromString(principal.userId());
    }
}
