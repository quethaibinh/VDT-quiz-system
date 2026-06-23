package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.teachers.ResolveTeachersRequestDTO;
import com.auth_service.auth_service.model.dto.teachers.ResolveTeachersResponseDTO;
import com.auth_service.auth_service.service.teachers.TeacherResolutionService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/internal/auth-service/teachers")
public class InternalTeacherController {

    private final TeacherResolutionService teacherResolutionService;

    public InternalTeacherController(TeacherResolutionService teacherResolutionService) {
        this.teacherResolutionService = teacherResolutionService;
    }

    @PostMapping("/resolve")
    public ResolveTeachersResponseDTO resolve(@RequestBody ResolveTeachersRequestDTO request) {
        return teacherResolutionService.resolve(request);
    }
}
