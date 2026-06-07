package com.auth_service.auth_service.service.imports;

import com.auth_service.auth_service.model.dto.imports.ImportUserErrorDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserResultDTO;
import com.auth_service.auth_service.model.dto.imports.ImportUserRowDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserImportServiceTests {

    @Mock
    private ExcelUserParser excelUserParser;

    @Mock
    private ImportUserValidator importUserValidator;

    @Mock
    private ImportedUserCreationService importedUserCreationService;

    @InjectMocks
    private UserImportService userImportService;

    @Test
    void importsValidRowsAndReturnsInvalidRowsWithoutRollback() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "users.xlsx", "application/octet-stream", new byte[]{1});
        ImportUserRowDTO validRow = row(2, "SV001", null);
        ImportUserRowDTO invalidRow = row(3, null, null);
        ImportUserErrorDTO error = new ImportUserErrorDTO(
                3,
                "studentCode",
                "MISSING_CODE",
                "Student code or teacher code is required"
        );

        when(excelUserParser.parse(file)).thenReturn(List.of(validRow, invalidRow));
        when(importUserValidator.validate(
                any(ImportUserRowDTO.class),
                any(Set.class),
                any(Set.class),
                any(Set.class)
        )).thenReturn(Optional.empty(), Optional.of(error));

        ImportUserResultDTO result = userImportService.importUsers(file);

        assertThat(result.getTotalRows()).isEqualTo(2);
        assertThat(result.getSuccessCount()).isEqualTo(1);
        assertThat(result.getFailedCount()).isEqualTo(1);
        assertThat(result.getErrors()).containsExactly(error);
        verify(importedUserCreationService).createUser(validRow);
        verify(importedUserCreationService, never()).createUser(invalidRow);
    }

    private ImportUserRowDTO row(int rowNumber, String studentCode, String teacherCode) {
        return new ImportUserRowDTO(
                rowNumber,
                null,
                null,
                "Test User",
                "test@gmail.com",
                studentCode,
                teacherCode,
                null,
                null,
                null,
                null
        );
    }
}
