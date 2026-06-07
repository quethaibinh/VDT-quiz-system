package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserErrorDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.Checker;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportUserValidatorTests {

    private final UserRepo userRepo = mock(UserRepo.class);
    private final ImportUserValidator validator = new ImportUserValidator(userRepo, new Checker());

    @Test
    void reportsDuplicateStudentCodeInDatabase() {
        when(userRepo.existsByStudentCode("SV001")).thenReturn(true);

        Optional<ImportUserErrorDTO> error = validator.validate(
                row("SV001", null),
                new HashSet<>(),
                new HashSet<>(),
                new HashSet<>()
        );

        assertThat(error).isPresent();
        assertThat(error.get().getFieldName()).isEqualTo("studentCode");
        assertThat(error.get().getErrorCode()).isEqualTo("DUPLICATE_STUDENT_CODE");
    }

    @Test
    void reportsDuplicateGeneratedUsernameInFile() {
        HashSet<String> seenUsernames = new HashSet<>();
        HashSet<String> seenStudentCodes = new HashSet<>();
        HashSet<String> seenTeacherCodes = new HashSet<>();

        assertThat(validator.validate(
                row("SV001", null),
                seenUsernames,
                seenStudentCodes,
                seenTeacherCodes
        )).isEmpty();

        Optional<ImportUserErrorDTO> error = validator.validate(
                row("SV001", null),
                seenUsernames,
                seenStudentCodes,
                seenTeacherCodes
        );

        assertThat(error).isPresent();
        assertThat(error.get().getErrorCode()).isEqualTo("DUPLICATE_USERNAME_IN_FILE");
    }

    private ImportUserRowDTO row(String studentCode, String teacherCode) {
        return new ImportUserRowDTO(
                2,
                null,
                null,
                "Test User",
                null,
                studentCode,
                teacherCode,
                null,
                null,
                null,
                null
        );
    }
}
