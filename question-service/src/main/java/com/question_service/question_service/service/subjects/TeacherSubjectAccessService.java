package com.question_service.question_service.service.subjects;

import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
/**
 * Kiem tra giao vien co phan cong ACTIVE tren mon hoc dang hoat dong.
 */
public class TeacherSubjectAccessService {

    private final SubjectRepo subjectRepo;
    private final SubjectTeacherRepo subjectTeacherRepo;

    public TeacherSubjectAccessService(SubjectRepo subjectRepo, SubjectTeacherRepo subjectTeacherRepo) {
        this.subjectRepo = subjectRepo;
        this.subjectTeacherRepo = subjectTeacherRepo;
    }

    /**
     * Tra ve mon hoc khi quyen hop le, nguoc lai dung request bang ma loi phu hop.
     */
    public Subject requireActiveAssignment(UUID subjectId, UUID teacherId) {
        Subject subject = subjectRepo.findById(subjectId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "SUBJECT_NOT_FOUND"));
        if (!SubjectStatus.ACTIVE.equals(subject.getStatus())) {
            throw error(HttpStatus.BAD_REQUEST, "SUBJECT_NOT_ACTIVE");
        }
        if (!subjectTeacherRepo.existsBySubjectIdAndTeacherIdAndStatus(
                subjectId,
                teacherId,
                SubjectTeacherStatus.ACTIVE
        )) {
            throw error(HttpStatus.FORBIDDEN, "SUBJECT_FORBIDDEN");
        }
        return subject;
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
