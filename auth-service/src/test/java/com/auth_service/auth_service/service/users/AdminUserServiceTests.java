package com.auth_service.auth_service.service.users;

import com.auth_service.auth_service.model.dto.users.UpdateAdminUserRequestDTO;
import com.auth_service.auth_service.model.dto.users.UpdateUserStatusRequestDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserGender;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserServiceTests {

    private final UserRepo userRepo = mock(UserRepo.class);
    private final AdminUserService service = new AdminUserService(userRepo);

    @Test
    void searchesManagedUsersWithValidatedFiltersAndSafeDtos() {
        UserEntity teacher = user(UserType.TEACHER, "ACTIVE");
        teacher.setPasswordHash("secret");
        teacher.setPhoneEncrypt("encrypted");
        when(userRepo.searchAdminUsers(eq(UserType.TEACHER), eq("ACTIVE"), eq("an"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(teacher)));

        var result = service.search("teacher", "active", " An ", 0, 20, "fullName,desc");

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().id()).isEqualTo(teacher.getId());
        assertThat(result.content().getFirst().email()).isEqualTo(teacher.getEmail());
        verify(userRepo).searchAdminUsers(eq(UserType.TEACHER), eq("ACTIVE"), eq("an"), any());
    }

    @Test
    void rejectsAdminTargetsAndInvalidFilters() {
        UserEntity admin = user(UserType.ADMIN, "ACTIVE");
        when(userRepo.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.get(admin.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ADMIN_USER_MANAGEMENT_FORBIDDEN");
        assertThatThrownBy(() -> service.search("ADMIN", null, null, 0, 20, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_USER_TYPE");
        assertThatThrownBy(() -> service.search(null, "LOCKED", null, 0, 20, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_USER_STATUS");
        assertThatThrownBy(() -> service.search(null, null, null, 0, 101, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_PAGE_REQUEST");
    }

    @Test
    void rejectsNullOrFutureUserTypesOutsideManagedBoundary() {
        UserEntity unknown = user(UserType.TEACHER, "ACTIVE");
        unknown.setUserType(null);
        when(userRepo.findById(unknown.getId())).thenReturn(Optional.of(unknown));

        assertThatThrownBy(() -> service.get(unknown.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("ADMIN_USER_MANAGEMENT_FORBIDDEN");
    }

    @Test
    void updatesOnlySafeProfileFieldsAndStatus() {
        UserEntity teacher = user(UserType.TEACHER, "ACTIVE");
        when(userRepo.findById(teacher.getId())).thenReturn(Optional.of(teacher));
        when(userRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var updated = service.update(teacher.getId(), new UpdateAdminUserRequestDTO(
                " New Name ", " Display ", "new@example.com",
                LocalDate.of(1990, 5, 20), UserGender.MALE
        ));
        var inactive = service.updateStatus(
                teacher.getId(),
                new UpdateUserStatusRequestDTO("inactive")
        );

        assertThat(updated.fullName()).isEqualTo("New Name");
        assertThat(updated.displayName()).isEqualTo("Display");
        assertThat(inactive.status()).isEqualTo("INACTIVE");
    }

    @Test
    void usesCountQueriesForStatistics() {
        when(userRepo.count()).thenReturn(10L);
        when(userRepo.countByUserTypeInAndStatusIgnoreCase(
                List.of(UserType.TEACHER, UserType.STUDENT),
                "ACTIVE"
        )).thenReturn(6L);
        when(userRepo.countByUserTypeInAndStatusIgnoreCase(
                List.of(UserType.TEACHER, UserType.STUDENT),
                "INACTIVE"
        )).thenReturn(2L);
        when(userRepo.countByUserTypeAndStatusIgnoreCase(UserType.TEACHER, "ACTIVE")).thenReturn(2L);
        when(userRepo.countByUserTypeAndStatusIgnoreCase(UserType.STUDENT, "ACTIVE")).thenReturn(4L);

        var result = service.statistics();

        assertThat(result.totalUsers()).isEqualTo(10);
        assertThat(result.activeUsers()).isEqualTo(6);
        assertThat(result.inactiveUsers()).isEqualTo(2);
        assertThat(result.activeTeachers()).isEqualTo(2);
        assertThat(result.activeStudents()).isEqualTo(4);
    }

    @Test
    void rejectsLegacyUnknownStatusInsteadOfMisrepresentingIt() {
        UserEntity teacher = user(UserType.TEACHER, "LOCKED");
        when(userRepo.findById(teacher.getId())).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> service.get(teacher.getId()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("INVALID_USER_STATUS");
    }

    private UserEntity user(UserType type, String status) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setUsername("user");
        user.setUserType(type);
        user.setTeacherCode(type == UserType.TEACHER ? "GV001" : null);
        user.setStudentCode(type == UserType.STUDENT ? "SV001" : null);
        user.setFullName("Nguyen Van An");
        user.setDisplayName("An");
        user.setEmail("an@example.com");
        user.setStatus(status);
        return user;
    }
}
