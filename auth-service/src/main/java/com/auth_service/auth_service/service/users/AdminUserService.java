package com.auth_service.auth_service.service.users;

import com.auth_service.auth_service.model.dto.users.AdminUserDetailDTO;
import com.auth_service.auth_service.model.dto.users.AdminUserPageResponseDTO;
import com.auth_service.auth_service.model.dto.users.AdminUserStatisticsDTO;
import com.auth_service.auth_service.model.dto.users.AdminUserSummaryDTO;
import com.auth_service.auth_service.model.dto.users.UpdateAdminUserRequestDTO;
import com.auth_service.auth_service.model.dto.users.UpdateUserStatusRequestDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class AdminUserService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> STATUSES = Set.of("ACTIVE", "INACTIVE");
    private static final List<UserType> MANAGED_USER_TYPES = List.of(
            UserType.TEACHER,
            UserType.STUDENT
    );
    private static final Set<String> SORT_FIELDS = Set.of(
            "username", "studentCode", "teacherCode", "fullName", "displayName",
            "email", "createdAt", "status"
    );
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UserRepo userRepo;

    public AdminUserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public AdminUserPageResponseDTO search(
            String userTypeValue,
            String statusValue,
            String keyword,
            int page,
            int size,
            String sortValue
    ) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("INVALID_PAGE_REQUEST");
        }
        UserType userType = parseUserType(userTypeValue);
        String status = parseOptionalStatus(statusValue);
        Sort sort = parseSort(sortValue);
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        return AdminUserPageResponseDTO.from(userRepo.searchAdminUsers(
                userType,
                status,
                normalizedKeyword,
                PageRequest.of(page, size, sort)
        ).map(this::toSummary));
    }

    public AdminUserDetailDTO get(UUID userId) {
        return toDetail(requireManageableUser(userId));
    }

    @Transactional
    public AdminUserDetailDTO update(UUID userId, UpdateAdminUserRequestDTO request) {
        UserEntity user = requireManageableUser(userId);
        if (request == null || request.fullName() == null || request.fullName().isBlank()) {
            throw new IllegalArgumentException("FULL_NAME_REQUIRED");
        }
        String email = trimToNull(request.email());
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw new IllegalArgumentException("INVALID_EMAIL");
        }
        if (request.birthDate() != null && request.birthDate().isAfter(java.time.LocalDate.now())) {
            throw new IllegalArgumentException("INVALID_BIRTH_DATE");
        }
        user.setFullName(request.fullName().trim());
        user.setDisplayName(trimToNull(request.displayName()));
        user.setEmail(email);
        user.setBirthDate(request.birthDate());
        user.setGender(request.gender());
        return toDetail(userRepo.save(user));
    }

    @Transactional
    public AdminUserDetailDTO updateStatus(UUID userId, UpdateUserStatusRequestDTO request) {
        UserEntity user = requireManageableUser(userId);
        if (request == null) {
            throw new IllegalArgumentException("INVALID_USER_STATUS");
        }
        user.setStatus(parseRequiredStatus(request.status()));
        return toDetail(userRepo.save(user));
    }

    public AdminUserStatisticsDTO statistics() {
        return new AdminUserStatisticsDTO(
                userRepo.count(),
                userRepo.countByUserTypeInAndStatusIgnoreCase(MANAGED_USER_TYPES, "ACTIVE"),
                userRepo.countByUserTypeInAndStatusIgnoreCase(MANAGED_USER_TYPES, "INACTIVE"),
                userRepo.countByUserTypeAndStatusIgnoreCase(UserType.TEACHER, "ACTIVE"),
                userRepo.countByUserTypeAndStatusIgnoreCase(UserType.STUDENT, "ACTIVE")
        );
    }

    private UserEntity requireManageableUser(UUID userId) {
        UserEntity user = userRepo.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));
        if (user.getUserType() == null || !MANAGED_USER_TYPES.contains(user.getUserType())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "ADMIN_USER_MANAGEMENT_FORBIDDEN");
        }
        if (user.getStatus() == null || !STATUSES.contains(user.getStatus().toUpperCase(Locale.ROOT))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "INVALID_USER_STATUS");
        }
        return user;
    }

    private UserType parseUserType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            UserType type = UserType.valueOf(value.trim().toUpperCase(Locale.ROOT));
            if (type == UserType.ADMIN) {
                throw new IllegalArgumentException("INVALID_USER_TYPE");
            }
            return type;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("INVALID_USER_TYPE");
        }
    }

    private String parseOptionalStatus(String value) {
        return value == null || value.isBlank() ? null : parseRequiredStatus(value);
    }

    private String parseRequiredStatus(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        if (!STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("INVALID_USER_STATUS");
        }
        return normalized;
    }

    private Sort parseSort(String value) {
        String[] parts = value == null || value.isBlank()
                ? new String[]{"fullName", "asc"}
                : value.split(",", 2);
        String field = parts[0].trim();
        if (!SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("INVALID_SORT_FIELD");
        }
        try {
            Sort.Direction direction = parts.length == 2
                    ? Sort.Direction.fromString(parts[1].trim())
                    : Sort.Direction.ASC;
            return Sort.by(direction, field).and(Sort.by(Sort.Direction.ASC, "id"));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("INVALID_SORT_DIRECTION");
        }
    }

    private AdminUserSummaryDTO toSummary(UserEntity user) {
        return new AdminUserSummaryDTO(
                user.getId(), user.getUsername(), user.getUserType(), user.getStudentCode(),
                user.getTeacherCode(), user.getFullName(), user.getDisplayName(), user.getEmail(),
                user.getStatus()
        );
    }

    private AdminUserDetailDTO toDetail(UserEntity user) {
        return new AdminUserDetailDTO(
                user.getId(), user.getUsername(), user.getUserType(), user.getStudentCode(),
                user.getTeacherCode(), user.getFullName(), user.getDisplayName(), user.getEmail(),
                user.getBirthDate(), user.getGender(), user.getAvatarUrl(), user.getStatus(),
                user.getCreatedAt(), user.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
