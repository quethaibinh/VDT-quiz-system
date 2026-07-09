package com.result_service.result_service.controller;

import com.result_service.result_service.model.dto.internal.ResultStatusRequestDTO;
import com.result_service.result_service.model.dto.internal.ResultStatusResponseDTO;
import com.result_service.result_service.service.results.InternalResultStatusService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InternalResultStatusControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void statusDtoAcceptsJsonBodyShapeUsedByRuntimeService() throws Exception {
        UUID submissionId = UUID.randomUUID();

        ResultStatusRequestDTO request = objectMapper.readValue(
                """
                        {"submissionIds":["%s"]}
                        """.formatted(submissionId),
                ResultStatusRequestDTO.class
        );

        assertThat(request.getSubmissionIds()).containsExactly(submissionId);
    }

    @Test
    void statusReturnsGradedSubmissionIds() {
        UUID submissionId = UUID.randomUUID();
        InternalResultStatusService statusService = mock(InternalResultStatusService.class);
        when(statusService.existingSubmissionIds(List.of(submissionId))).thenReturn(List.of(submissionId));
        InternalResultStatusController controller = new InternalResultStatusController(statusService);

        ResultStatusResponseDTO response = controller.status(new ResultStatusRequestDTO(List.of(submissionId)));

        assertThat(response.getGradedSubmissionIds()).containsExactly(submissionId);
    }
}
