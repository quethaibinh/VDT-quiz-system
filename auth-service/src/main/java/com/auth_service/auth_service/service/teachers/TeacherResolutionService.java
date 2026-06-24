package com.auth_service.auth_service.service.teachers;

import com.auth_service.auth_service.model.dto.teachers.ResolveTeachersRequestDTO;
import com.auth_service.auth_service.model.dto.teachers.ResolveTeachersResponseDTO;
import com.auth_service.auth_service.model.dto.teachers.TeacherSummaryDTO;
import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import com.auth_service.auth_service.repository.UserRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class TeacherResolutionService {

    private static final int MAX_IDS = 100;
    private final UserRepo userRepo;

    public TeacherResolutionService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    public ResolveTeachersResponseDTO resolve(ResolveTeachersRequestDTO request) {
        List<UUID> requestedIds = parseIds(request);
        Map<UUID, UserEntity> usersById = new LinkedHashMap<>();
        userRepo.findAllByIdIn(requestedIds).forEach(user -> usersById.put(user.getId(), user));

        List<TeacherSummaryDTO> teachers = new ArrayList<>();
        List<UUID> missingIds = new ArrayList<>();
        List<UUID> nonTeacherIds = new ArrayList<>();
        for (UUID id : requestedIds) {
            UserEntity user = usersById.get(id);
            if (user == null) {
                missingIds.add(id);
            } else if (user.getUserType() != UserType.TEACHER) {
                nonTeacherIds.add(id);
            } else {
                teachers.add(new TeacherSummaryDTO(
                        user.getId(), user.getTeacherCode(), user.getFullName(),
                        user.getDisplayName(), user.getEmail(), user.getStatus()
                ));
            }
        }
        return new ResolveTeachersResponseDTO(teachers, missingIds, nonTeacherIds);
    }

    private List<UUID> parseIds(ResolveTeachersRequestDTO request) {
        if (request == null || request.teacherIds() == null || request.teacherIds().isEmpty()) {
            throw new IllegalArgumentException("TEACHER_IDS_REQUIRED");
        }
        if (request.teacherIds().size() > MAX_IDS) {
            throw new IllegalArgumentException("TOO_MANY_TEACHER_IDS");
        }
        List<UUID> ids = new ArrayList<>(request.teacherIds().size());
        for (String value : request.teacherIds()) {
            try {
                ids.add(UUID.fromString(value));
            } catch (RuntimeException exception) {
                throw new IllegalArgumentException("INVALID_TEACHER_ID");
            }
        }
        return ids;
    }
}
