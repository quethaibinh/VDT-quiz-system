package com.exam_service.exam_service.controller;

import com.exam_service.exam_service.model.dto.assignments.InternalExamAssignmentDTO;
import com.exam_service.exam_service.service.exams.ExamAssignmentSnapshotService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/internal/exam-service/exams/{examId}")
// Controller noi bo dung cho microservice khac (nhu runtime-service) truy van phan cong
public class InternalExamAssignmentController {

    private final ExamAssignmentSnapshotService assignmentSnapshotService;

    public InternalExamAssignmentController(ExamAssignmentSnapshotService assignmentSnapshotService) {
        this.assignmentSnapshotService = assignmentSnapshotService;
    }

    @GetMapping("/assignments")
    // Api lay danh sach hoc sinh va assignmentId tuong ung lam fallback khi Redis bi miss
    public InternalExamAssignmentDTO getAssignments(@PathVariable UUID examId) {
        return assignmentSnapshotService.getAssignmentsSnapshot(examId);
    }
}
