package com.question_service.question_service.service.subjects;

import com.question_service.question_service.model.dto.subjects.SubjectRequestDTO;
import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.repository.SubjectRepo;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubjectServiceTests {

    private final SubjectRepo subjectRepo = mock(SubjectRepo.class);
    private final SubjectService service = new SubjectService(subjectRepo);
    private final UUID adminId = UUID.randomUUID();

    @Test
    void createsActiveSubjectWithNormalizedCode() {
        when(subjectRepo.existsByCodeIgnoreCase("PHI101")).thenReturn(false);
        when(subjectRepo.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject subject = invocation.getArgument(0);
            subject.setId(UUID.randomUUID());
            return subject;
        });

        SubjectResponseDTO response = service.createSubject(
                adminId,
                new SubjectRequestDTO(" phi101 ", " Philosophy 101 ", " Intro ")
        );

        assertThat(response.getCode()).isEqualTo("PHI101");
        assertThat(response.getName()).isEqualTo("Philosophy 101");
        assertThat(response.getDescription()).isEqualTo("Intro");
        assertThat(response.getStatus()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(response.getCreatedByAdminId()).isEqualTo(adminId);
    }

    @Test
    void rejectsDuplicateSubjectCode() {
        when(subjectRepo.existsByCodeIgnoreCase("PHI101")).thenReturn(true);

        assertThatThrownBy(() -> service.createSubject(
                adminId,
                new SubjectRequestDTO("PHI101", "Philosophy 101", null)
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("DUPLICATE_SUBJECT_CODE");
    }

    @Test
    void archivesAndRestoresSubject() {
        UUID subjectId = UUID.randomUUID();
        Subject subject = subject(subjectId, "PHI101", SubjectStatus.ACTIVE);

        when(subjectRepo.findById(subjectId)).thenReturn(Optional.of(subject));
        when(subjectRepo.save(any(Subject.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubjectResponseDTO archived = service.archiveSubject(adminId, subjectId);
        SubjectResponseDTO restored = service.restoreSubject(adminId, subjectId);

        assertThat(archived.getStatus()).isEqualTo(SubjectStatus.ARCHIVED);
        assertThat(restored.getStatus()).isEqualTo(SubjectStatus.ACTIVE);
        assertThat(restored.getUpdatedByAdminId()).isEqualTo(adminId);
    }

    private Subject subject(UUID id, String code, String status) {
        Subject subject = new Subject();
        subject.setId(id);
        subject.setCode(code);
        subject.setName("Philosophy");
        subject.setStatus(status);
        return subject;
    }

}
