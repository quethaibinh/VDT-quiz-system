package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.dto.assignments.InternalExamAssignmentDTO;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
// Service lay thong tin danh sach hoc sinh duoc phan cong thi
public class ExamAssignmentSnapshotService {

    private final ExamAssignmentRepo examAssignmentRepo;

    public ExamAssignmentSnapshotService(ExamAssignmentRepo examAssignmentRepo) {
        this.examAssignmentRepo = examAssignmentRepo;
    }

    @Transactional(readOnly = true)
    // Lay danh sach tat ca phan cong voi trang thai ASSIGNED de lam phuong an fallback
    public InternalExamAssignmentDTO getAssignmentsSnapshot(UUID examId) {
        List<InternalExamAssignmentDTO.AssignmentDetail> details = examAssignmentRepo
                .findAllByExamIdAndStatus(examId, AssignmentStatus.ASSIGNED)
                .stream()
                .map(assignment -> InternalExamAssignmentDTO.AssignmentDetail.builder()
                        .assignmentId(assignment.getId())
                        .studentId(assignment.getStudentId())
                        .studentCode(assignment.getStudentCodeSnapshot())
                        .studentName(assignment.getStudentNameSnapshot())
                        .build())
                .collect(Collectors.toList());

        return InternalExamAssignmentDTO.builder()
                .examId(examId)
                .assignments(details)
                .build();
    }
}
