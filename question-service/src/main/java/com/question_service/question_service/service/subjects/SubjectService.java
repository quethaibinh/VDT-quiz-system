package com.question_service.question_service.service.subjects;

import com.question_service.question_service.model.dto.subjects.SubjectRequestDTO;
import com.question_service.question_service.model.dto.subjects.SubjectResponseDTO;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.repository.SubjectRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class SubjectService {

    private final SubjectRepo subjectRepo;

    public SubjectService(SubjectRepo subjectRepo) {
        this.subjectRepo = subjectRepo;
    }

    public SubjectResponseDTO createSubject(UUID adminId, SubjectRequestDTO request) {
        String code = requireText(request.getCode(), "code");
        String name = requireText(request.getName(), "name");
        String normalizedCode = normalizeCode(code);

        if (subjectRepo.existsByCodeIgnoreCase(normalizedCode)) {
            throw badRequest("DUPLICATE_SUBJECT_CODE");
        }

        Subject subject = new Subject();
        subject.setCode(normalizedCode);
        subject.setName(name);
        subject.setDescription(trimToNull(request.getDescription()));
        subject.setStatus(SubjectStatus.ACTIVE);
        subject.setCreatedByAdminId(adminId);
        subject.setUpdatedByAdminId(adminId);

        return toResponse(subjectRepo.save(subject));
    }

    public List<SubjectResponseDTO> listSubjects(String status, String keyword) {
        String normalizedStatus = normalizeStatus(status);
        String normalizedKeyword = trimToNull(keyword);
        List<Subject> subjects = normalizedStatus == null
                ? subjectRepo.findAll()
                : subjectRepo.findByStatus(normalizedStatus);

        return subjects.stream()
                .filter(subject -> matchesKeyword(subject, normalizedKeyword))
                .sorted(Comparator.comparing(Subject::getCode, String.CASE_INSENSITIVE_ORDER))
                .map(this::toResponse)
                .toList();
    }

    public SubjectResponseDTO getSubject(UUID subjectId) {
        return toResponse(loadSubject(subjectId));
    }

    public SubjectResponseDTO updateSubject(UUID adminId, UUID subjectId, SubjectRequestDTO request) {
        Subject subject = loadSubject(subjectId);
        String code = requireText(request.getCode(), "code");
        String name = requireText(request.getName(), "name");
        String normalizedCode = normalizeCode(code);

        if (subjectRepo.existsByCodeIgnoreCaseAndIdNot(normalizedCode, subjectId)) {
            throw badRequest("DUPLICATE_SUBJECT_CODE");
        }

        subject.setCode(normalizedCode);
        subject.setName(name);
        subject.setDescription(trimToNull(request.getDescription()));
        subject.setUpdatedByAdminId(adminId);

        return toResponse(subjectRepo.save(subject));
    }

    public SubjectResponseDTO archiveSubject(UUID adminId, UUID subjectId) {
        Subject subject = loadSubject(subjectId);
        subject.setStatus(SubjectStatus.ARCHIVED);
        subject.setUpdatedByAdminId(adminId);
        return toResponse(subjectRepo.save(subject));
    }

    public SubjectResponseDTO restoreSubject(UUID adminId, UUID subjectId) {
        Subject subject = loadSubject(subjectId);
        subject.setStatus(SubjectStatus.ACTIVE);
        subject.setUpdatedByAdminId(adminId);
        return toResponse(subjectRepo.save(subject));
    }

    Subject loadSubject(UUID subjectId) {
        return subjectRepo.findById(subjectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND"));
    }

    SubjectResponseDTO toResponse(Subject subject) {
        return new SubjectResponseDTO(
                subject.getId(),
                subject.getCode(),
                subject.getName(),
                subject.getDescription(),
                subject.getStatus(),
                subject.getCreatedByAdminId(),
                subject.getUpdatedByAdminId(),
                subject.getCreatedAt(),
                subject.getUpdatedAt(),
                null
        );
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

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeStatus(String status) {
        String normalizedStatus = trimToNull(status);
        return normalizedStatus == null ? null : normalizedStatus.toUpperCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            throw badRequest("REQUIRED_FIELD:" + fieldName);
        }
        return trimmed;
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private ResponseStatusException badRequest(String reason) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, reason);
    }

}
