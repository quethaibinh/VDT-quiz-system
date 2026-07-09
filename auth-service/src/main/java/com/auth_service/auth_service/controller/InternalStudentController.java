package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.students.ResolveStudentsRequestDTO;
import com.auth_service.auth_service.model.dto.students.ResolveStudentsResponseDTO;
import com.auth_service.auth_service.service.students.StudentDiscoveryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal/auth-service/students")
public class InternalStudentController {

    private final StudentDiscoveryService studentDiscoveryService;

    public InternalStudentController(StudentDiscoveryService studentDiscoveryService) {
        this.studentDiscoveryService = studentDiscoveryService;
    }

    @PostMapping("/resolve")
    public ResolveStudentsResponseDTO resolve(@RequestBody ResolveStudentsRequestDTO request) {
        return studentDiscoveryService.resolve(request);
    }
}
