package com.result_service.result_service.service.results;

import com.result_service.result_service.repository.ExamResultRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Service
public class InternalResultStatusService {

    private final ExamResultRepo examResultRepo;

    public InternalResultStatusService(ExamResultRepo examResultRepo) {
        this.examResultRepo = examResultRepo;
    }

    @Transactional(readOnly = true)
    public List<UUID> existingSubmissionIds(Collection<UUID> submissionIds) {
        if (submissionIds == null || submissionIds.isEmpty()) {
            return List.of();
        }
        return examResultRepo.findExistingSubmissionIds(submissionIds);
    }
}
