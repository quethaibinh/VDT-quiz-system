package com.auth_service.auth_service.service.users;

import com.auth_service.auth_service.model.dto.users.UpdatePasswordRequestDTO;
import com.auth_service.auth_service.model.dto.users.UpdateUserProfileRequestDTO;
import com.auth_service.auth_service.model.dto.users.UserProfileResponseDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.repository.UserRepo;
import com.auth_service.auth_service.util.Checker;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ProfileService {

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final Checker checker;

    public ProfileService(UserRepo userRepo, PasswordEncoder passwordEncoder, Checker checker) {
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
        this.checker = checker;
    }

    public UserProfileResponseDTO getUserProfile(UUID userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        return toResponseDTO(user);
    }

    @Transactional
    public UserProfileResponseDTO updateUserProfile(UUID userId, UpdateUserProfileRequestDTO request) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("FULL_NAME_REQUIRED");
        }

        String email = trimToNull(request.email());
        if (email != null) {
            if (!EMAIL.matcher(email).matches()) {
                throw new IllegalArgumentException("INVALID_EMAIL_FORMAT");
            }
            if (!checker.emailChecker(email)) {
                throw new IllegalArgumentException("EMAIL_MUST_BE_GMAIL");
            }
        }

        if (request.birthDate() != null && request.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("INVALID_BIRTH_DATE");
        }

        user.setFullName(request.fullName().trim());
        user.setDisplayName(trimToNull(request.displayName()));
        user.setEmail(email);
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());

        return toResponseDTO(userRepo.save(user));
    }

    @Transactional
    public void updatePassword(UUID userId, UpdatePasswordRequestDTO request) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        if (request.currentPassword() == null || request.currentPassword().isBlank()) {
            throw new IllegalArgumentException("CURRENT_PASSWORD_REQUIRED");
        }
        if (request.newPassword() == null || request.newPassword().isBlank()) {
            throw new IllegalArgumentException("NEW_PASSWORD_REQUIRED");
        }
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INCORRECT_CURRENT_PASSWORD");
        }
        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("CONFIRM_PASSWORD_MISMATCH");
        }
        if (!checker.passwordCheck(request.newPassword())) {
            throw new IllegalArgumentException("PASSWORD_NOT_STRONG");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepo.save(user);
    }

    private UserProfileResponseDTO toResponseDTO(UserEntity user) {
        return new UserProfileResponseDTO(
                user.getId(),
                user.getUsername(),
                user.getUserType(),
                user.getStudentCode(),
                user.getTeacherCode(),
                user.getFullName(),
                user.getDisplayName(),
                user.getEmail(),
                user.getBirthDate(),
                user.getGender(),
                user.getAvatarUrl()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
