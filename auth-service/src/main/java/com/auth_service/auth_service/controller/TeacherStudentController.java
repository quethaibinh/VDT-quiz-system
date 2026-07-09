package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.students.StudentPageResponseDTO;
import com.auth_service.auth_service.service.students.StudentDiscoveryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/api/auth-service/teacher/students")
public class TeacherStudentController {

    private final StudentDiscoveryService studentDiscoveryService;

    public TeacherStudentController(StudentDiscoveryService studentDiscoveryService) {
        this.studentDiscoveryService = studentDiscoveryService;
    }

    @GetMapping
    public StudentPageResponseDTO search(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "studentCode,asc") String sort
    ) {
        return studentDiscoveryService.search(keyword, page, size, sort);
    }
}
