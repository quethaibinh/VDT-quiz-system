package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.SubjectTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubjectTeacherRepo extends JpaRepository<SubjectTeacher, UUID> {

    Optional<SubjectTeacher> findBySubjectIdAndTeacherId(UUID subjectId, UUID teacherId);

    boolean existsBySubjectIdAndTeacherIdAndStatus(UUID subjectId, UUID teacherId, String status);

    List<SubjectTeacher> findBySubjectIdAndStatus(UUID subjectId, String status);

    List<SubjectTeacher> findByTeacherIdAndStatus(UUID teacherId, String status);

}
