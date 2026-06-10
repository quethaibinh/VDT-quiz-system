package com.question_service.question_service.service.subjects;

import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.model.dto.subjects.SubjectTeacherResponseDTO;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacher;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubjectTeacherServiceTests {

    private final SubjectRepo subjectRepo = mock(SubjectRepo.class);
    private final SubjectTeacherRepo subjectTeacherRepo = mock(SubjectTeacherRepo.class);
    private final SubjectService subjectService = new SubjectService(subjectRepo);
    private final SubjectTeacherService service = new SubjectTeacherService(
            subjectRepo,
            subjectTeacherRepo,
            subjectService
    );
    private final UUID subjectId = UUID.randomUUID();
    private final UUID teacherId = UUID.randomUUID();
    private final UUID adminId = UUID.randomUUID();

    @Test
    void assignsTeacherToSubject() {
        when(subjectRepo.existsById(subjectId)).thenReturn(true);
        when(subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId)).thenReturn(Optional.empty());
        when(subjectTeacherRepo.save(any(SubjectTeacher.class))).thenAnswer(invocation -> {
            SubjectTeacher assignment = invocation.getArgument(0);
            assignment.setId(UUID.randomUUID());
            return assignment;
        });

        SubjectTeacherResponseDTO response = service.assignTeacher(subjectId, teacherId, adminId);

        assertThat(response.getSubjectId()).isEqualTo(subjectId);
        assertThat(response.getTeacherId()).isEqualTo(teacherId);
        assertThat(response.getStatus()).isEqualTo(SubjectTeacherStatus.ACTIVE);
        assertThat(response.getAssignedByAdminId()).isEqualTo(adminId);
        assertThat(response.getAssignedAt()).isNotNull();
    }

    @Test
    void rejectsDuplicateActiveAssignment() {
        SubjectTeacher existing = assignment(SubjectTeacherStatus.ACTIVE);
        when(subjectRepo.existsById(subjectId)).thenReturn(true);
        when(subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId))
                .thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.assignTeacher(subjectId, teacherId, adminId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("TEACHER_ALREADY_ASSIGNED");
    }

    @Test
    void teacherListOnlyReturnsAssignedActiveSubjects() {
        Subject activeSubject = subject(subjectId, "PHI101", SubjectStatus.ACTIVE);
        Subject archivedSubject = subject(UUID.randomUUID(), "OLD101", SubjectStatus.ARCHIVED);
        SubjectTeacher assignment = assignment(SubjectTeacherStatus.ACTIVE);

        when(subjectTeacherRepo.findByTeacherIdAndStatus(teacherId, SubjectTeacherStatus.ACTIVE))
                .thenReturn(List.of(assignment));
        when(subjectRepo.findAllById(any())).thenReturn(List.of(activeSubject, archivedSubject));

        List<SubjectResponseDTO> responses = service.listSubjectsForTeacher(teacherId, null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().getId()).isEqualTo(subjectId);
        assertThat(responses.getFirst().getAssignedAt()).isEqualTo(assignment.getAssignedAt());
    }

    @Test
    void teacherListDoesNotExposeArchivedSubjectsByStatusFilter() {
        List<SubjectResponseDTO> responses = service.listSubjectsForTeacher(
                teacherId,
                SubjectStatus.ARCHIVED,
                null
        );

        assertThat(responses).isEmpty();
    }

    @Test
    void teacherCannotViewUnassignedSubject() {
        Subject activeSubject = subject(subjectId, "PHI101", SubjectStatus.ACTIVE);
        when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(activeSubject));
        when(subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getSubjectForTeacher(teacherId, subjectId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("SUBJECT_FORBIDDEN");
    }

    private SubjectTeacher assignment(String status) {
        SubjectTeacher assignment = new SubjectTeacher();
        assignment.setId(UUID.randomUUID());
        assignment.setSubjectId(subjectId);
        assignment.setTeacherId(teacherId);
        assignment.setStatus(status);
        assignment.setAssignedByAdminId(adminId);
        assignment.setAssignedAt(LocalDateTime.now());
        return assignment;
    }

    private Subject subject(UUID id, String code, String status) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode(code);
        subject.setName(code + " name");
        subject.setStatus(status);
        return subject;
    }

}
