package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.CryptoUtil;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ImportedUserCreationServiceTests {

    @Test
    void createsStudentWithCodeAsUsernameAndEncodedPassword() throws Exception {
        UserRepo userRepo = mock(UserRepo.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        CryptoUtil cryptoUtil = mock(CryptoUtil.class);
        ImportedUserCreationService service = new ImportedUserCreationService(
                userRepo,
                passwordEncoder,
                cryptoUtil
        );
        ImportUserRowDTO row = new ImportUserRowDTO(
                2,
                "ignored-username",
                "ignored-password",
                "Nguyen Van A",
                "a@gmail.com",
                "SV001",
                null,
                null,
                null,
                "2004-01-02",
                "MALE"
        );
        when(passwordEncoder.encode("SV001")).thenReturn("encoded-password");

        service.createUser(row);

        ArgumentCaptor<UserEntity> captor = ArgumentCaptor.forClass(UserEntity.class);
        verify(userRepo).save(captor.capture());
        UserEntity savedUser = captor.getValue();

        assertThat(savedUser.getUsername()).isEqualTo("SV001");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encoded-password");
        assertThat(savedUser.getUserType()).isEqualTo(UserType.STUDENT);
        assertThat(savedUser.getStudentCode()).isEqualTo("SV001");
        assertThat(savedUser.getStatus()).isEqualTo("ACTIVE");
    }
}
