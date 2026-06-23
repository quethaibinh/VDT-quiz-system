package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.model.dto.users.AdminUserDetailDTO;
import com.auth_service.auth_service.model.dto.users.AdminUserPageResponseDTO;
import com.auth_service.auth_service.model.dto.users.AdminUserStatisticsDTO;
import com.auth_service.auth_service.model.dto.users.UpdateAdminUserRequestDTO;
import com.auth_service.auth_service.model.dto.users.UpdateUserStatusRequestDTO;
import com.auth_service.auth_service.service.users.AdminUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/admin/auth-service/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public AdminUserPageResponseDTO search(
            @RequestParam(required = false) String userType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "fullName,asc") String sort
    ) {
        return adminUserService.search(userType, status, keyword, page, size, sort);
    }

    @GetMapping("/statistics")
    public AdminUserStatisticsDTO statistics() {
        return adminUserService.statistics();
    }

    @GetMapping("/{userId}")
    public AdminUserDetailDTO get(@PathVariable UUID userId) {
        return adminUserService.get(userId);
    }

    @PutMapping("/{userId}")
    public AdminUserDetailDTO update(
            @PathVariable UUID userId,
            @RequestBody UpdateAdminUserRequestDTO request
    ) {
        return adminUserService.update(userId, request);
    }

    @PatchMapping("/{userId}/status")
    public AdminUserDetailDTO updateStatus(
            @PathVariable UUID userId,
            @RequestBody UpdateUserStatusRequestDTO request
    ) {
        return adminUserService.updateStatus(userId, request);
    }
}
