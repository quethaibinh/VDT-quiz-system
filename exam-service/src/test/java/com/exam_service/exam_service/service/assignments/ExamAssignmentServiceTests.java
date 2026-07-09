package com.exam_service.exam_service.service.assignments;

import com.exam_service.exam_service.client.AuthServiceClient;
import com.exam_service.exam_service.client.ResolvedStudent;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamAssignment;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.service.exams.TeacherExamDraftService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamAssignmentServiceTests {

    private final ExamAssignmentRepo assignmentRepo = mock(ExamAssignmentRepo.class);
    private final TeacherExamDraftService examService = mock(TeacherExamDraftService.class);
    private final AuthServiceClient authClient = mock(AuthServiceClient.class);
    private ExamAssignmentService service;

    @BeforeEach
    void setUp() {
        service = new ExamAssignmentService(assignmentRepo, examService, authClient);
    }

    @Test
    void addsNewStudentAndUsesAuthSnapshot() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(examId);
        when(examService.requireEditable(subjectId, examId, teacherId)).thenReturn(exam);
        when(authClient.resolveStudents(List.of(studentId))).thenReturn(List.of(
                new ResolvedStudent(studentId, "S001", "Student One", null)
        ));
        when(assignmentRepo.findAllByExamIdAndStudentIdIn(examId, List.of(studentId)))
                .thenReturn(List.of());
        when(assignmentRepo.saveAll(anyList())).thenAnswer(invocation -> {
            List<ExamAssignment> values = invocation.getArgument(0);
            values.forEach(value -> value.setId(UUID.randomUUID()));
            return values;
        });

        var result = service.add(subjectId, examId, teacherId, List.of(studentId));

        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.assignments().getFirst().studentCode()).isEqualTo("S001");
        assertThat(result.assignments().getFirst().studentName()).isEqualTo("Student One");
    }

    @Test
    void duplicateActiveAssignmentIsIdempotent() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(examId);
        ExamAssignment existing = new ExamAssignment();
        existing.setId(UUID.randomUUID());
        existing.setExamId(examId);
        existing.setStudentId(studentId);
        existing.setStatus(AssignmentStatus.ASSIGNED);
        when(examService.requireEditable(subjectId, examId, teacherId)).thenReturn(exam);
        when(authClient.resolveStudents(List.of(studentId))).thenReturn(List.of(
                new ResolvedStudent(studentId, "S001", "Student One", null)
        ));
        when(assignmentRepo.findAllByExamIdAndStudentIdIn(examId, List.of(studentId)))
                .thenReturn(List.of(existing));
        when(assignmentRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.add(subjectId, examId, teacherId, List.of(studentId));

        assertThat(result.assignedCount()).isZero();
        assertThat(result.unchangedCount()).isEqualTo(1);
    }

    @Test
    void removesActiveAssignmentAndRecordsRemoval() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ExamAssignment assignment = assignment(examId, studentId, AssignmentStatus.ASSIGNED);
        when(assignmentRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.of(assignment));
        when(assignmentRepo.save(assignment)).thenReturn(assignment);

        var result = service.remove(subjectId, examId, teacherId, studentId);

        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.unchangedCount()).isZero();
        assertThat(assignment.getStatus()).isEqualTo(AssignmentStatus.REMOVED);
        assertThat(assignment.getRemovedAt()).isNotNull();
        verify(examService).requireEditable(subjectId, examId, teacherId);
        verify(assignmentRepo).save(assignment);
    }

    @Test
    void removingMissingOrAlreadyRemovedAssignmentIsIdempotent() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        when(assignmentRepo.findByExamIdAndStudentId(examId, studentId))
                .thenReturn(Optional.empty());

        var result = service.remove(subjectId, examId, teacherId, studentId);

        assertThat(result.assignedCount()).isZero();
        assertThat(result.unchangedCount()).isEqualTo(1);
        verify(assignmentRepo, never()).save(any());
    }

    @Test
    void reactivatesRemovedAssignmentAndRefreshesStudentSnapshot() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(examId);
        ExamAssignment removed = assignment(examId, studentId, AssignmentStatus.REMOVED);
        removed.setStudentCodeSnapshot("OLD");
        removed.setStudentNameSnapshot("Old Name");
        removed.setRemovedAt(java.time.LocalDateTime.now().minusDays(1));
        when(examService.requireEditable(subjectId, examId, teacherId)).thenReturn(exam);
        when(authClient.resolveStudents(List.of(studentId))).thenReturn(List.of(
                new ResolvedStudent(studentId, "S009", "New Name", null)
        ));
        when(assignmentRepo.findAllByExamIdAndStudentIdIn(examId, List.of(studentId)))
                .thenReturn(List.of(removed));
        when(assignmentRepo.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.add(subjectId, examId, teacherId, List.of(studentId));

        assertThat(result.assignedCount()).isEqualTo(1);
        assertThat(result.unchangedCount()).isZero();
        assertThat(removed.getStatus()).isEqualTo(AssignmentStatus.ASSIGNED);
        assertThat(removed.getRemovedAt()).isNull();
        assertThat(removed.getStudentCodeSnapshot()).isEqualTo("S009");
        assertThat(removed.getStudentNameSnapshot()).isEqualTo("New Name");
        assertThat(removed.getAssignedBy()).isEqualTo(teacherId);
    }

    @Test
    void rejectsIncompleteResolvedStudentSetWithoutSaving() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID firstStudent = UUID.randomUUID();
        UUID secondStudent = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(examId);
        when(examService.requireEditable(subjectId, examId, teacherId)).thenReturn(exam);
        when(authClient.resolveStudents(List.of(firstStudent, secondStudent))).thenReturn(List.of(
                new ResolvedStudent(firstStudent, "S001", "Student One", null)
        ));

        assertThatThrownBy(() -> service.add(
                subjectId, examId, teacherId, List.of(firstStudent, secondStudent)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_STUDENT_SELECTION");

        verify(assignmentRepo, never()).saveAll(anyList());
    }

    @Test
    void rejectsResolvedStudentsWithUnexpectedIdsWithoutSaving() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID requestedStudent = UUID.randomUUID();
        UUID unexpectedStudent = UUID.randomUUID();
        Exam exam = new Exam();
        exam.setId(examId);
        when(examService.requireEditable(subjectId, examId, teacherId)).thenReturn(exam);
        when(authClient.resolveStudents(List.of(requestedStudent))).thenReturn(List.of(
                new ResolvedStudent(unexpectedStudent, "S999", "Wrong Student", null)
        ));

        assertThatThrownBy(() -> service.add(
                subjectId, examId, teacherId, List.of(requestedStudent)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_STUDENT_SELECTION");

        verify(assignmentRepo, never()).saveAll(anyList());
    }

    private ExamAssignment assignment(
            UUID examId,
            UUID studentId,
            AssignmentStatus status
    ) {
        ExamAssignment assignment = new ExamAssignment();
        assignment.setId(UUID.randomUUID());
        assignment.setExamId(examId);
        assignment.setStudentId(studentId);
        assignment.setStudentCodeSnapshot("S001");
        assignment.setStudentNameSnapshot("Student One");
        assignment.setStatus(status);
        assignment.setAssignedAt(java.time.LocalDateTime.now().minusHours(1));
        return assignment;
    }
}
