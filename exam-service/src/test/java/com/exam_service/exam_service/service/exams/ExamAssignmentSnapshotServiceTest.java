package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.ExamAssignment;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ExamAssignmentSnapshotServiceTest {

    @Test
    void getAssignmentsSnapshotIncludesStudentDisplayFields() {
        UUID examId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        ExamAssignment assignment = new ExamAssignment();
        assignment.setId(assignmentId);
        assignment.setExamId(examId);
        assignment.setStudentId(studentId);
        assignment.setStudentCodeSnapshot("S001");
        assignment.setStudentNameSnapshot("Student One");
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        ExamAssignmentRepo repo = mock(ExamAssignmentRepo.class);
        when(repo.findAllByExamIdAndStatus(examId, AssignmentStatus.ASSIGNED)).thenReturn(List.of(assignment));

        var snapshot = new ExamAssignmentSnapshotService(repo).getAssignmentsSnapshot(examId);

        assertThat(snapshot.getAssignments()).hasSize(1);
        assertThat(snapshot.getAssignments().getFirst())
                .satisfies(detail -> {
                    assertThat(detail.getAssignmentId()).isEqualTo(assignmentId);
                    assertThat(detail.getStudentId()).isEqualTo(studentId);
                    assertThat(detail.getStudentCode()).isEqualTo("S001");
                    assertThat(detail.getStudentName()).isEqualTo("Student One");
                });
    }
}
