package com.auth_service.auth_service.service.teachers;

import com.auth_service.auth_service.model.dto.teachers.ResolveTeachersRequestDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TeacherResolutionServiceTests {

    private final UserRepo userRepo = mock(UserRepo.class);
    private final TeacherResolutionService service = new TeacherResolutionService(userRepo);

    @Test
    void preservesOrderAndReturnsInactiveTeachersWithRejectionBuckets() {
        UUID inactiveId = UUID.randomUUID();
        UUID missingId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID activeId = UUID.randomUUID();
        when(userRepo.findAllByIdIn(any())).thenReturn(List.of(
                user(activeId, UserType.TEACHER, "ACTIVE"),
                user(studentId, UserType.STUDENT, "ACTIVE"),
                user(inactiveId, UserType.TEACHER, "INACTIVE")
        ));

        var result = service.resolve(new ResolveTeachersRequestDTO(List.of(
                inactiveId.toString(), missingId.toString(), studentId.toString(), activeId.toString()
        )));

        assertThat(result.teachers()).extracting(value -> value.id())
                .containsExactly(inactiveId, activeId);
        assertThat(result.teachers().getFirst().status()).isEqualTo("INACTIVE");
        assertThat(result.missingTeacherIds()).containsExactly(missingId);
        assertThat(result.nonTeacherIds()).containsExactly(studentId);
    }

    private UserEntity user(UUID id, UserType type, String status) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUserType(type);
        user.setTeacherCode(type == UserType.TEACHER ? "GV001" : null);
        user.setFullName("Name");
        user.setDisplayName("Display");
        user.setEmail("teacher@example.com");
        user.setStatus(status);
        return user;
    }
}
