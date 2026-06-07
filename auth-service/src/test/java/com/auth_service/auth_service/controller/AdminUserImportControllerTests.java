package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.imports.ImportUserResultDTO;
import com.auth_service.auth_service.service.imports.UserImportService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminUserImportControllerTests {

    private final UserImportService userImportService = mock(UserImportService.class);
    private final AdminUserImportController controller = new AdminUserImportController(userImportService);
    private final MockMultipartFile file = new MockMultipartFile(
            "file",
            "users.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[]{1}
    );

    @Test
    void delegatesImportToService() throws Exception {
        ImportUserResultDTO expected = new ImportUserResultDTO();
        when(userImportService.importUsers(file)).thenReturn(expected);

        ImportUserResultDTO result = controller.importUsers(file);

        assertThat(result).isSameAs(expected);
        verify(userImportService).importUsers(file);
    }
}
