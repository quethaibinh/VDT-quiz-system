package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.dto.exams.StudentExamAvailability;
import com.exam_service.exam_service.model.dto.exams.StudentExamDetailDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamPageDTO;
import com.exam_service.exam_service.model.dto.exams.StudentExamSummaryDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;

@Service
/**
 * Dich vu doc danh sach va chi tiet ca thi danh cho hoc sinh.
 */
public class StudentExamReadService {

    private final ExamRepo examRepo;
    private final Clock clock;

    public StudentExamReadService(ExamRepo examRepo, Clock clock) {
        this.examRepo = examRepo;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    /**
     * Lay danh sach ca thi duoc phan cong cho hoc sinh.
     */
    public StudentExamPageDTO list(
            UUID studentId,
            String availabilityFilter,
            int page,
            int size
    ) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        
        // Chuan hoa gia tri bo loc neu co
        String parsedFilter = null;
        if (availabilityFilter != null && !availabilityFilter.isBlank()) {
            parsedFilter = availabilityFilter.trim().toUpperCase();
        }

        PageRequest pageRequest = PageRequest.of(page, size, Sort.by("startAt").ascending());
        Page<StudentExamSummaryDTO> dtoPage = examRepo.findAssignedExamsForStudent(
                studentId,
                parsedFilter,
                now,
                pageRequest
        ).map(exam -> toSummaryDTO(exam, now));

        return StudentExamPageDTO.from(dtoPage, now);
    }

    @Transactional(readOnly = true)
    /**
     * Lay chi tiet mot ca thi duoc phan cong cho hoc sinh.
     */
    public StudentExamDetailDTO get(UUID examId, UUID studentId) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        Exam exam = examRepo.findAssignedExamForStudent(examId, studentId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND"));
        
        return toDetailDTO(exam, now);
    }

    private StudentExamSummaryDTO toSummaryDTO(Exam exam, OffsetDateTime now) {
        return new StudentExamSummaryDTO(
                exam.getId(),
                exam.getCode(),
                exam.getTitle(),
                exam.getSubjectNameSnapshot(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes(),
                exam.getEasyCount() + exam.getMediumCount() + exam.getHardCount(),
                exam.getStatus(),
                calculateAvailability(exam, now),
                AssignmentStatus.ASSIGNED
        );
    }

    private StudentExamDetailDTO toDetailDTO(Exam exam, OffsetDateTime now) {
        return new StudentExamDetailDTO(
                exam.getId(),
                exam.getCode(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubjectNameSnapshot(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes(),
                exam.getEasyCount() + exam.getMediumCount() + exam.getHardCount(),
                exam.getStatus(),
                calculateAvailability(exam, now),
                AssignmentStatus.ASSIGNED
        );
    }

    private StudentExamAvailability calculateAvailability(Exam exam, OffsetDateTime now) {
        if (exam.getStatus() == ExamStatus.CLOSED) {
            return StudentExamAvailability.ENDED;
        } else if (now.isBefore(exam.getStartAt())) {
            return StudentExamAvailability.UPCOMING;
        } else if (exam.getEndAt() != null && now.isAfter(exam.getEndAt())) {
            return StudentExamAvailability.ENDED;
        } else {
            return StudentExamAvailability.OPEN;
        }
    }
}
