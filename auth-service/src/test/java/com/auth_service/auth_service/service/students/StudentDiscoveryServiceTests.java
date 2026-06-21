package com.auth_service.auth_service.service.students;

import com.auth_service.auth_service.model.dto.students.ResolveStudentsRequestDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class StudentDiscoveryServiceTests {

    private final UserRepo userRepo = mock(UserRepo.class);
    private final StudentDiscoveryService service = new StudentDiscoveryService(userRepo);

    @Test
    void searchesOnlyActiveStudentsWithNormalizedKeywordAndSafePage() {
        UserEntity student = user(UUID.randomUUID(), UserType.STUDENT, "ACTIVE", "SV001", "Nguyen Van A");
        when(userRepo.searchStudents(eq(UserType.STUDENT), eq("ACTIVE"), eq("nguyen"), any()))
                .thenReturn(new PageImpl<>(List.of(student)));

        var result = service.search("  NGUYEN  ", 0, 20, "fullName,desc");

        assertThat(result.content()).containsExactly(
                new com.auth_service.auth_service.model.dto.students.StudentSummaryDTO(
                        student.getId(), "SV001", "Nguyen Van A", "Nguyen A"
                )
        );
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(userRepo).searchStudents(eq(UserType.STUDENT), eq("ACTIVE"), eq("nguyen"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        assertThat(pageable.getValue().getSort().getOrderFor("fullName").isDescending()).isTrue();
        assertThat(pageable.getValue().getSort().getOrderFor("id")).isNotNull();
    }

    @Test
    void rejectsUnsafePageAndSortValues() {
        assertThatThrownBy(() -> service.search("", 0, 101, "studentCode,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_PAGE_REQUEST");
        assertThatThrownBy(() -> service.search("", 0, 20, "email,asc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_SORT_FIELD");
        assertThatThrownBy(() -> service.search("", 0, 20, "studentCode,sideways"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_SORT_DIRECTION");
    }

    @Test
    void resolvesInInputOrderAndReportsRejectedUsers() {
        UUID activeId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UserEntity active = user(activeId, UserType.STUDENT, "ACTIVE", "SV001", "Active Student");
        UserEntity inactive = user(inactiveId, UserType.STUDENT, "INACTIVE", "SV002", "Inactive Student");
        UserEntity teacher = user(teacherId, UserType.TEACHER, "ACTIVE", null, "Teacher");
        when(userRepo.findAllByIdIn(any())).thenReturn(List.of(teacher, inactive, active));

        var result = service.resolve(new ResolveStudentsRequestDTO(List.of(
                missingId.toString(),
                activeId.toString(),
                inactiveId.toString(),
                teacherId.toString()
        )));

        assertThat(result.students()).extracting(student -> student.id()).containsExactly(activeId);
        assertThat(result.missingStudentIds()).containsExactly(missingId);
        assertThat(result.inactiveStudentIds()).containsExactly(inactiveId);
        assertThat(result.nonStudentIds()).containsExactly(teacherId);
    }

    @Test
    void rejectsInvalidAndOversizedBulkRequests() {
        assertThatThrownBy(() -> service.resolve(new ResolveStudentsRequestDTO(List.of("not-a-uuid"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_STUDENT_ID");

        List<String> ids = java.util.stream.IntStream.range(0, 101)
                .mapToObj(index -> UUID.randomUUID().toString())
                .toList();
        assertThatThrownBy(() -> service.resolve(new ResolveStudentsRequestDTO(ids)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("TOO_MANY_STUDENT_IDS");
    }

    private UserEntity user(
            UUID id,
            UserType userType,
            String status,
            String studentCode,
            String fullName
    ) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUserType(userType);
        user.setStatus(status);
        user.setStudentCode(studentCode);
        user.setFullName(fullName);
        user.setDisplayName(fullName.equals("Nguyen Van A") ? "Nguyen A" : fullName);
        return user;
    }
}
