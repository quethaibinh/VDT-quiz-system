package com.auth_service.auth_service.service.students;

import com.auth_service.auth_service.model.dto.students.ResolveStudentsRequestDTO;
import com.auth_service.auth_service.model.dto.students.ResolveStudentsResponseDTO;
import com.auth_service.auth_service.model.dto.students.StudentPageResponseDTO;
import com.auth_service.auth_service.model.dto.students.StudentSummaryDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StudentDiscoveryService {

    static final int MAX_PAGE_SIZE = 100;
    static final int MAX_BULK_IDS = 100;
    private static final String ACTIVE_STATUS = "ACTIVE";
    private static final Set<String> SORT_FIELDS = Set.of("studentCode", "fullName", "displayName");

    private final UserRepo userRepo;

    public StudentDiscoveryService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public StudentPageResponseDTO search(String keyword, int page, int size, String sortValue) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("INVALID_PAGE_REQUEST");
        }

        String[] sortParts = sortValue == null || sortValue.isBlank()
                ? new String[]{"studentCode", "asc"}
                : sortValue.split(",", 2);
        String sortField = sortParts[0].trim();
        if (!SORT_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException("INVALID_SORT_FIELD");
        }

        Sort.Direction direction;
        try {
            direction = sortParts.length == 2
                    ? Sort.Direction.fromString(sortParts[1].trim())
                    : Sort.Direction.ASC;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("INVALID_SORT_DIRECTION");
        }

        Sort sort = Sort.by(direction, sortField).and(Sort.by(Sort.Direction.ASC, "id"));
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);

        return StudentPageResponseDTO.from(userRepo.searchStudents(
                UserType.STUDENT,
                ACTIVE_STATUS,
                normalizedKeyword,
                PageRequest.of(page, size, sort)
        ).map(this::toSummary));
    }

    public ResolveStudentsResponseDTO resolve(ResolveStudentsRequestDTO request) {
        List<UUID> requestedIds = parseRequestedIds(request);
        Map<UUID, UserEntity> usersById = new LinkedHashMap<>();
        userRepo.findAllByIdIn(requestedIds).forEach(user -> usersById.put(user.getId(), user));

        List<StudentSummaryDTO> students = new ArrayList<>();
        List<UUID> missingIds = new ArrayList<>();
        List<UUID> inactiveIds = new ArrayList<>();
        List<UUID> nonStudentIds = new ArrayList<>();

        for (UUID id : requestedIds) {
            UserEntity user = usersById.get(id);
            if (user == null) {
                missingIds.add(id);
            } else if (user.getUserType() != UserType.STUDENT) {
                nonStudentIds.add(id);
            } else if (!ACTIVE_STATUS.equals(user.getStatus())) {
                inactiveIds.add(id);
            } else {
                students.add(toSummary(user));
            }
        }

        return new ResolveStudentsResponseDTO(students, missingIds, inactiveIds, nonStudentIds);
    }

    private List<UUID> parseRequestedIds(ResolveStudentsRequestDTO request) {
        if (request == null || request.studentIds() == null || request.studentIds().isEmpty()) {
            throw new IllegalArgumentException("STUDENT_IDS_REQUIRED");
        }
        if (request.studentIds().size() > MAX_BULK_IDS) {
            throw new IllegalArgumentException("TOO_MANY_STUDENT_IDS");
        }

        List<UUID> ids = new ArrayList<>(request.studentIds().size());
        for (String value : request.studentIds()) {
            try {
                ids.add(UUID.fromString(value));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("INVALID_STUDENT_ID");
            }
        }
        return ids;
    }

    private StudentSummaryDTO toSummary(UserEntity user) {
        return new StudentSummaryDTO(
                user.getId(),
                user.getStudentCode(),
                user.getFullName(),
                user.getDisplayName()
        );
    }
}
