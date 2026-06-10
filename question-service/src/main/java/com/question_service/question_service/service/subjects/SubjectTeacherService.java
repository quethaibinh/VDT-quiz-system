package com.question_service.question_service.service.subjects;

import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.model.dto.subjects.SubjectTeacherResponseDTO;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacher;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SubjectTeacherService {

    private final SubjectRepo subjectRepo;
    private final SubjectTeacherRepo subjectTeacherRepo;
    private final SubjectService subjectService;

    public SubjectTeacherService(
            SubjectRepo subjectRepo,
            SubjectTeacherRepo subjectTeacherRepo,
            SubjectService subjectService
    ) {
        this.subjectRepo = subjectRepo;
        this.subjectTeacherRepo = subjectTeacherRepo;
        this.subjectService = subjectService;
    }

    public SubjectTeacherResponseDTO assignTeacher(UUID subjectId, UUID teacherId, UUID adminId) {
        requireTeacherId(teacherId);
        requireSubjectExists(subjectId);

        SubjectTeacher assignment = subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId)
                .orElseGet(SubjectTeacher::new);

        if (assignment.getId() != null && SubjectTeacherStatus.ACTIVE.equals(assignment.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEACHER_ALREADY_ASSIGNED");
        }

        assignment.setSubjectId(subjectId);
        assignment.setTeacherId(teacherId);
        assignment.setStatus(SubjectTeacherStatus.ACTIVE);
        assignment.setAssignedByAdminId(adminId);
        assignment.setAssignedAt(LocalDateTime.now());
        assignment.setDeletedAt(null);

        return toResponse(subjectTeacherRepo.save(assignment));
    }

    public List<SubjectTeacherResponseDTO> listTeachers(UUID subjectId) {
        requireSubjectExists(subjectId);
        return subjectTeacherRepo.findBySubjectIdAndStatus(subjectId, SubjectTeacherStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(SubjectTeacher::getAssignedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(this::toResponse)
                .toList();
    }

    public void removeTeacher(UUID subjectId, UUID teacherId) {
        SubjectTeacher assignment = subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "TEACHER_SUBJECT_ASSIGNMENT_NOT_FOUND"
                ));

        assignment.setStatus(SubjectTeacherStatus.INACTIVE);
        assignment.setDeletedAt(LocalDateTime.now());
        subjectTeacherRepo.save(assignment);
    }

    public List<SubjectResponseDTO> listSubjectsForTeacher(UUID teacherId, String status, String keyword) {
        requireTeacherId(teacherId);
        String requestedStatus = trimToNull(status);
        String normalizedStatus = requestedStatus == null
                ? SubjectStatus.ACTIVE
                : requestedStatus.toUpperCase(Locale.ROOT);
        if (!SubjectStatus.ACTIVE.equals(normalizedStatus)) {
            return List.of();
        }
        String normalizedKeyword = trimToNull(keyword);

        List<SubjectTeacher> assignments = subjectTeacherRepo.findByTeacherIdAndStatus(
                teacherId,
                SubjectTeacherStatus.ACTIVE
        );
        Map<UUID, SubjectTeacher> assignmentBySubjectId = assignments.stream()
                .collect(Collectors.toMap(SubjectTeacher::getSubjectId, assignment -> assignment));

        return subjectRepo.findAllById(assignmentBySubjectId.keySet()).stream()
                .filter(subject -> normalizedStatus.equals(subject.getStatus()))
                .filter(subject -> matchesKeyword(subject, normalizedKeyword))
                .sorted(Comparator.comparing(Subject::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(subject -> toTeacherSubjectResponse(subject, assignmentBySubjectId.get(subject.getId())))
                .toList();
    }

    public SubjectResponseDTO getSubjectForTeacher(UUID teacherId, UUID subjectId) {
        requireTeacherId(teacherId);
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND"));

        if (!SubjectStatus.ACTIVE.equals(subject.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUBJECT_NOT_ACTIVE");
        }

        SubjectTeacher assignment = subjectTeacherRepo.findBySubjectIdAndTeacherId(subjectId, teacherId)
                .filter(value -> SubjectTeacherStatus.ACTIVE.equals(value.getStatus()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "SUBJECT_FORBIDDEN"));

        return toTeacherSubjectResponse(subject, assignment);
    }

    private void requireSubjectExists(UUID subjectId) {
        if (!subjectRepo.existsById(subjectId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND");
        }
    }

    private void requireTeacherId(UUID teacherId) {
        if (teacherId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD:teacherId");
        }
    }

    private SubjectTeacherResponseDTO toResponse(SubjectTeacher assignment) {
        return new SubjectTeacherResponseDTO(
                assignment.getId(),
                assignment.getSubjectId(),
                assignment.getTeacherId(),
                assignment.getStatus(),
                assignment.getAssignedByAdminId(),
                assignment.getAssignedAt()
        );
    }

    private SubjectResponseDTO toTeacherSubjectResponse(Subject subject, SubjectTeacher assignment) {
        SubjectResponseDTO response = subjectService.toResponse(subject);
        response.setAssignedAt(assignment.getAssignedAt());
        return response;
    }

    private boolean matchesKeyword(Subject subject, String keyword) {
        if (keyword == null) {
            return true;
        }

        String lowerKeyword = keyword.toLowerCase(Locale.ROOT);
        return containsIgnoreCase(subject.getCode(), lowerKeyword)
                || containsIgnoreCase(subject.getName(), lowerKeyword)
                || containsIgnoreCase(subject.getDescription(), lowerKeyword);
    }

    private boolean containsIgnoreCase(String value, String lowerKeyword) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(lowerKeyword);
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

}
