package com.auth_service.auth_service.controller;

import com.auth_service.auth_service.config.security.GatewayUserPrincipal;
import com.auth_service.auth_service.model.dto.users.UpdatePasswordRequestDTO;
import com.auth_service.auth_service.model.dto.users.UpdateUserProfileRequestDTO;
import com.auth_service.auth_service.model.dto.users.UserProfileResponseDTO;
import com.auth_service.auth_service.service.users.ProfileService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/api/auth-service/profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    public UserProfileResponseDTO getProfile(@AuthenticationPrincipal GatewayUserPrincipal principal) {
        UUID userId = UUID.fromString(principal.userId());
        return profileService.getUserProfile(userId);
    }

    @PutMapping
    public UserProfileResponseDTO updateProfile(
            @AuthenticationPrincipal GatewayUserPrincipal principal,
            @RequestBody UpdateUserProfileRequestDTO request
    ) {
        UUID userId = UUID.fromString(principal.userId());
        return profileService.updateUserProfile(userId, request);
    }

    @PutMapping("/password")
    public void updatePassword(
            @AuthenticationPrincipal GatewayUserPrincipal principal,
            @RequestBody UpdatePasswordRequestDTO request
    ) {
        UUID userId = UUID.fromString(principal.userId());
        profileService.updatePassword(userId, request);
    }
}
