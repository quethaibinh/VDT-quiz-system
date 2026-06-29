package com.result_service.result_service.controller;

import com.result_service.result_service.model.dto.internal.ResultStatusRequestDTO;
import com.result_service.result_service.model.dto.internal.ResultStatusResponseDTO;
import com.result_service.result_service.service.results.InternalResultStatusService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/internal/result-service/results")
public class InternalResultStatusController {

    private final InternalResultStatusService statusService;

    public InternalResultStatusController(InternalResultStatusService statusService) {
        this.statusService = statusService;
    }

    @PostMapping("/status")
    public ResultStatusResponseDTO status(@RequestBody ResultStatusRequestDTO request) {
        List<UUID> existing = statusService.existingSubmissionIds(
                request != null ? request.getSubmissionIds() : List.of()
        );
        return new ResultStatusResponseDTO(existing);
    }
}
