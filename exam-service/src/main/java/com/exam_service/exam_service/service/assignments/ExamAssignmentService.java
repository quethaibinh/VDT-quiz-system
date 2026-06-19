package com.exam_service.exam_service.service.assignments;

import com.exam_service.exam_service.client.AuthServiceClient;
import com.exam_service.exam_service.client.ResolvedStudent;
import com.exam_service.exam_service.model.dto.assignments.AssignmentMutationResultDTO;
import com.exam_service.exam_service.model.dto.assignments.AssignmentResponseDTO;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamAssignment;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.service.exams.TeacherExamDraftService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * Quan ly danh sach hoc sinh duoc phep tham gia ca thi.
 * Client chi gui student ID; snapshot danh tinh luon duoc lay lai tu Auth Service.
 */
public class ExamAssignmentService {

    private final ExamAssignmentRepo assignmentRepo;
    private final TeacherExamDraftService examService;
    private final AuthServiceClient authServiceClient;

    public ExamAssignmentService(
            ExamAssignmentRepo assignmentRepo,
            TeacherExamDraftService examService,
            AuthServiceClient authServiceClient
    ) {
        this.assignmentRepo = assignmentRepo;
        this.examService = examService;
        this.authServiceClient = authServiceClient;
    }

    @Transactional
    /**
     * Them hoac kich hoat lai phan cong da REMOVED theo cach idempotent tuan tu.
     */
    public AssignmentMutationResultDTO add(
            UUID subjectId,
            UUID examId,
            UUID teacherId,
            List<UUID> requestedStudentIds
    ) {
        Exam exam = examService.requireEditable(subjectId, examId, teacherId);
        // Loai ID trung nhung van giu thu tu nguoi dung da chon.
        List<UUID> studentIds = new ArrayList<>(new LinkedHashSet<>(requestedStudentIds));
        List<ResolvedStudent> resolved = authServiceClient.resolveStudents(studentIds);
        // Toan bo request bi tu choi neu co bat ky ID nao khong hop le.
        if (resolved.size() != studentIds.size()) {
            throw new IllegalArgumentException("INVALID_STUDENT_SELECTION");
        }
        Map<UUID, ResolvedStudent> studentsById = resolved.stream()
                .collect(Collectors.toMap(ResolvedStudent::id, Function.identity()));
        if (!studentsById.keySet().containsAll(studentIds)) {
            throw new IllegalArgumentException("INVALID_STUDENT_SELECTION");
        }

        Map<UUID, ExamAssignment> existingByStudent = assignmentRepo
                .findAllByExamIdAndStudentIdIn(examId, studentIds)
                .stream()
                .collect(Collectors.toMap(ExamAssignment::getStudentId, Function.identity()));

        int changed = 0;
        int unchanged = 0;
        List<ExamAssignment> assignments = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (UUID studentId : studentIds) {
            ResolvedStudent student = studentsById.get(studentId);
            ExamAssignment assignment = existingByStudent.get(studentId);
            if (assignment == null) {
                assignment = new ExamAssignment();
                assignment.setExamId(exam.getId());
                assignment.setStudentId(studentId);
                assignment.setAssignedBy(teacherId);
                assignment.setAssignedAt(now);
                changed++;
            } else if (assignment.getStatus() == AssignmentStatus.ASSIGNED) {
                // Request lap lai khong tao them dong phan cong.
                unchanged++;
            } else {
                // Tai kich hoat dong cu de giu mot lich su duy nhat cho exam va student.
                assignment.setAssignedBy(teacherId);
                assignment.setAssignedAt(now);
                changed++;
            }
            assignment.setStudentCodeSnapshot(student.studentCode());
            assignment.setStudentNameSnapshot(student.snapshotName());
            assignment.setStatus(AssignmentStatus.ASSIGNED);
            assignment.setRemovedAt(null);
            assignments.add(assignment);
        }
        List<AssignmentResponseDTO> saved = assignmentRepo.saveAll(assignments)
                .stream()
                .map(this::toResponse)
                .toList();
        return new AssignmentMutationResultDTO(studentIds.size(), changed, unchanged, saved);
    }

    @Transactional
    /**
     * Go hoc sinh theo soft lifecycle; go lap lai van tra ket qua thanh cong.
     */
    public AssignmentMutationResultDTO remove(
            UUID subjectId,
            UUID examId,
            UUID teacherId,
            UUID studentId
    ) {
        examService.requireEditable(subjectId, examId, teacherId);
        ExamAssignment assignment = assignmentRepo.findByExamIdAndStudentId(examId, studentId)
                .orElse(null);
        if (assignment == null || assignment.getStatus() == AssignmentStatus.REMOVED) {
            return new AssignmentMutationResultDTO(1, 0, 1, List.of());
        }
        assignment.setStatus(AssignmentStatus.REMOVED);
        assignment.setRemovedAt(LocalDateTime.now());
        return new AssignmentMutationResultDTO(
                1,
                1,
                0,
                List.of(toResponse(assignmentRepo.save(assignment)))
        );
    }

    @Transactional(readOnly = true)
    /**
     * Chi tra cac phan cong dang ASSIGNED va sap xep theo ma hoc sinh.
     */
    public PageResponseDTO<AssignmentResponseDTO> list(
            UUID subjectId,
            UUID examId,
            UUID teacherId,
            int page,
            int size
    ) {
        Exam exam = examService.requireEditable(subjectId, examId, teacherId);
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("INVALID_PAGE_REQUEST");
        }
        return PageResponseDTO.from(
                assignmentRepo.findAllByExamIdAndStatus(
                        exam.getId(),
                        AssignmentStatus.ASSIGNED,
                        PageRequest.of(page, size, Sort.by("studentCodeSnapshot").ascending())
                ).map(this::toResponse)
        );
    }

    private AssignmentResponseDTO toResponse(ExamAssignment assignment) {
        return new AssignmentResponseDTO(
                assignment.getId(),
                assignment.getStudentId(),
                assignment.getStudentCodeSnapshot(),
                assignment.getStudentNameSnapshot(),
                assignment.getStatus(),
                assignment.getAssignedAt(),
                assignment.getRemovedAt()
        );
    }
}
