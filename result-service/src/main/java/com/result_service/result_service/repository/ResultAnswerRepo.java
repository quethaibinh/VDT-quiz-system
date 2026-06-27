package com.result_service.result_service.repository;

import com.result_service.result_service.model.entity.ResultAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResultAnswerRepo extends JpaRepository<ResultAnswer, UUID> {
    List<ResultAnswer> findByResultId(UUID resultId);

    List<ResultAnswer> findByResultIdOrderByQuestionOrderAsc(UUID resultId);
}
