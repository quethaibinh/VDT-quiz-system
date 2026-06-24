package com.question_service.question_service.service.subjects;

import com.question_service.question_service.client.AuthServiceClient;
import com.question_service.question_service.client.ResolveTeachersResponse;
import com.question_service.question_service.client.TeacherSummary;
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
/**
 * Quan ly phan cong giao vien va danh sach mon hoc cua tung giao vien.
 */
public class SubjectTeacherService {

    private final SubjectRepo subjectRepo;
    private final SubjectTeacherRepo subjectTeacherRepo;
    private final SubjectService subjectService;
    private final AuthServiceClient authServiceClient;

    public SubjectTeacherService(
            SubjectRepo subjectRepo,
            SubjectTeacherRepo subjectTeacherRepo,
            SubjectService subjectService,
            AuthServiceClient authServiceClient
    ) {
        this.subjectRepo = subjectRepo;
        this.subjectTeacherRepo = subjectTeacherRepo;
        this.subjectService = subjectService;
        this.authServiceClient = authServiceClient;
    }

    /**
     * Tao moi hoac kich hoat lai phan cong da bi go truoc do.
     */
    public SubjectTeacherResponseDTO assignTeacher(UUID subjectId, UUID teacherId, UUID adminId) {
        requireTeacherId(teacherId);
        requireActiveSubject(subjectId);
        TeacherSummary teacher = requireActiveTeacher(teacherId);

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

        return toResponse(subjectTeacherRepo.save(assignment), teacher, "RESOLVED");
    }

    // lay danh sach giao vien duoc phan cong vao cac mon hoc => subjectId
    public List<SubjectTeacherResponseDTO> listTeachers(UUID subjectId) {
        requireSubjectExists(subjectId);
        List<SubjectTeacher> assignments = subjectTeacherRepo
                .findBySubjectIdAndStatus(subjectId, SubjectTeacherStatus.ACTIVE)
                .stream()
                .sorted(Comparator.comparing(SubjectTeacher::getAssignedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (assignments.isEmpty()) {
            return List.of();
        }
        // lay danh sach giao vien tu auth-service
        ResolveTeachersResponse resolved = authServiceClient.resolveTeachers(
                assignments.stream().map(SubjectTeacher::getTeacherId).toList()
        );
        Map<UUID, TeacherSummary> teachers = resolved.teachers().stream()
                .collect(Collectors.toMap(TeacherSummary::id, teacher -> teacher));
        // tra ra danh sach cac mon hoc duoc phan cong cho cac giao vien
        return assignments.stream()
                .map(assignment -> {
                    UUID teacherId = assignment.getTeacherId();
                    TeacherSummary teacher = teachers.get(teacherId);
                    String identityState = teacher != null
                            ? "RESOLVED"
                            : resolved.nonTeacherIds().contains(teacherId) ? "NON_TEACHER" : "MISSING";
                    return toResponse(assignment, teacher, identityState);
                })
                .toList();
    }

    /**
     * Go phan cong bang cach chuyen trang thai sang INACTIVE.
     */
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

    /**
     * Chi tra ve mon hoc dang hoat dong va co phan cong ACTIVE.
     */
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

    private void requireActiveSubject(UUID subjectId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND"));
        if (!SubjectStatus.ACTIVE.equals(subject.getStatus())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "SUBJECT_NOT_ACTIVE");
        }
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

    private TeacherSummary requireActiveTeacher(UUID teacherId) {
        ResolveTeachersResponse resolved = authServiceClient.resolveTeachers(List.of(teacherId));
        if (resolved.missingTeacherIds().contains(teacherId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEACHER_NOT_FOUND");
        }
        if (resolved.nonTeacherIds().contains(teacherId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "USER_NOT_TEACHER");
        }
        TeacherSummary teacher = resolved.teachers().stream()
                .filter(value -> teacherId.equals(value.id()))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "INVALID_AUTH_SERVICE_RESPONSE"
                ));
        if (!"ACTIVE".equalsIgnoreCase(teacher.status())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TEACHER_NOT_ACTIVE");
        }
        return teacher;
    }

    private SubjectTeacherResponseDTO toResponse(
            SubjectTeacher assignment,
            TeacherSummary teacher,
            String identityState
    ) {
        return new SubjectTeacherResponseDTO(
                assignment.getId(),
                assignment.getSubjectId(),
                assignment.getTeacherId(),
                assignment.getStatus(),
                assignment.getAssignedByAdminId(),
                assignment.getAssignedAt(),
                teacher == null ? null : teacher.teacherCode(),
                teacher == null ? null : teacher.fullName(),
                teacher == null ? null : teacher.displayName(),
                teacher == null ? null : teacher.email(),
                teacher == null ? null : teacher.status(),
                identityState
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
