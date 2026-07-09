package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.client.QuestionCollectionMetadata;
import com.exam_service.exam_service.client.QuestionServiceClient;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.model.dto.exams.ExamDetailDTO;
import com.exam_service.exam_service.model.dto.exams.ExamDraftRequestDTO;
import com.exam_service.exam_service.model.dto.exams.ExamSummaryDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.model.entity.enums.HandleViolation;
import com.exam_service.exam_service.model.entity.enums.ShowResultPolicy;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
/**
 * Xu ly vong doi ca thi nhap va cac quy tac cau hinh de thi.
 * Service nay chi luu blueprint; cau hoi cu the chi duoc snapshot khi activate sau nay.
 */
public class TeacherExamDraftService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "title", "startAt");

    private final ExamRepo examRepo;
    private final ExamAssignmentRepo assignmentRepo;
    private final QuestionServiceClient questionServiceClient;

    public TeacherExamDraftService(
            ExamRepo examRepo,
            ExamAssignmentRepo assignmentRepo,
            QuestionServiceClient questionServiceClient
    ) {
        this.examRepo = examRepo;
        this.assignmentRepo = assignmentRepo;
        this.questionServiceClient = questionServiceClient;
    }

    /**
     * Xac thuc bo cau hoi voi Question Service truoc khi tao ca thi DRAFT.
     */
    public ExamDetailDTO create(
            UUID subjectId,
            UUID teacherId,
            ExamDraftRequestDTO request
    ) {
        QuestionCollectionMetadata metadata = validateBlueprint(subjectId, teacherId, request);
        Exam exam = new Exam();
        exam.setCode(generateCode());
        exam.setSubjectId(subjectId);
        exam.setCreatedByTeacherId(teacherId);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setExamType(ExamType.STANDARD_EXAM);
        apply(exam, request, metadata);
        return toDetail(examRepo.save(exam));
    }

    @Transactional(readOnly = true)
    /**
     * Tim ca thi theo subject va owner de tranh lam lo ca thi cua giao vien khac.
     */
    public PageResponseDTO<ExamSummaryDTO> list(
            UUID subjectId,
            UUID teacherId,
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        Specification<Exam> specification = (root, query, cb) -> cb.and(
                cb.equal(root.get("subjectId"), subjectId),
                cb.equal(root.get("createdByTeacherId"), teacherId),
                cb.equal(root.get("examType"), ExamType.STANDARD_EXAM)
        );
        if (status != null && !status.isBlank()) {
            ExamStatus parsed = parse(status, ExamStatus.class, "INVALID_EXAM_STATUS");
            specification = specification.and(
                    (root, query, cb) -> cb.equal(root.get("status"), parsed)
            );
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
            ));
        }
        Page<ExamSummaryDTO> result = examRepo.findAll(
                specification,
                pageable(page, size, sort)
        ).map(this::toSummary);
        return PageResponseDTO.from(result);
    }

    @Transactional(readOnly = true)
    public ExamDetailDTO get(UUID subjectId, UUID examId, UUID teacherId) {
        return toDetail(requireOwned(subjectId, examId, teacherId));
    }

    /**
     * Cap nhat toan bo cau hinh khi ca thi van con o trang thai DRAFT.
     */
    public ExamDetailDTO update(
            UUID subjectId,
            UUID examId,
            UUID teacherId,
            ExamDraftRequestDTO request
    ) {
        Exam exam = requireEditable(subjectId, examId, teacherId);
        QuestionCollectionMetadata metadata = validateBlueprint(subjectId, teacherId, request);
        apply(exam, request, metadata);
        return toDetail(examRepo.save(exam));
    }

    /**
     * Chuyen DRAFT sang CANCELLED, giu nguyen cau hinh va lich su phan cong.
     */
    public ExamDetailDTO cancel(UUID subjectId, UUID examId, UUID teacherId) {
        Exam exam = requireEditable(subjectId, examId, teacherId);
        exam.setStatus(ExamStatus.CANCELLED);
        return toDetail(examRepo.save(exam));
    }

    /**
     * Bao ve moi thao tac thay doi; ca thi da roi DRAFT khong con duoc sua.
     */
    public Exam requireEditable(UUID subjectId, UUID examId, UUID teacherId) {
        Exam exam = requireOwned(subjectId, examId, teacherId);
        if (exam.getStatus() != ExamStatus.DRAFT) {
            throw new ConflictException("EXAM_NOT_EDITABLE");
        }
        return exam;
    }

    private Exam requireOwned(UUID subjectId, UUID examId, UUID teacherId) {
        // Gop subject va owner vao truy van de an su ton tai cua ca thi khong co quyen.
        return examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(examId, subjectId, teacherId)
                .orElseThrow(() -> new NotFoundException("EXAM_NOT_FOUND"));
    }

    /**
     * Doi chieu quota voi so cau dang ACTIVE va giao vien thuc su duoc phep dung.
     */
    private QuestionCollectionMetadata validateBlueprint(
            UUID subjectId,
            UUID teacherId,
            ExamDraftRequestDTO request
    ) {
        int total = request.easyCount() + request.mediumCount() + request.hardCount();
        if (total <= 0) {
            throw new IllegalArgumentException("QUESTION_QUOTA_REQUIRED");
        }
        QuestionCollectionMetadata metadata = questionServiceClient.getExamMetadata(
                subjectId,
                request.collectionId(),
                teacherId
        );
        if (!subjectId.equals(metadata.subjectId())) {
            throw new IllegalArgumentException("COLLECTION_SUBJECT_MISMATCH");
        }
        if (request.easyCount() > metadata.easy()
                || request.mediumCount() > metadata.medium()
                || request.hardCount() > metadata.hard()) {
            throw new IllegalArgumentException("INSUFFICIENT_COLLECTION_QUOTA");
        }
        return metadata;
    }

    private void apply(
            Exam exam,
            ExamDraftRequestDTO request,
            QuestionCollectionMetadata metadata
    ) {
        String title = request.title().trim();
        exam.setTitle(title);
        exam.setDescription(trimToNull(request.description()));
        // Snapshot ten mon va bo cau hoi de bao cao van dung khi du lieu goc doi ten.
        exam.setCollectionId(metadata.collectionId());
        exam.setCollectionNameSnapshot(metadata.name());
        exam.setSubjectNameSnapshot(metadata.subjectName());
        exam.setEasyCount(request.easyCount());
        exam.setMediumCount(request.mediumCount());
        exam.setHardCount(request.hardCount());
        exam.setStartAt(request.startAt());
        exam.setDurationMinutes(request.durationMinutes());
        exam.setEndAt(request.startAt().plusMinutes(request.durationMinutes()));
        exam.setJoinBeforeMinutes(request.joinBeforeMinutes());
        exam.setJoinAfterMinutes(request.joinAfterMinutes());
        exam.setShuffleQuestions(request.shuffleQuestions());
        exam.setShuffleOptions(request.shuffleOptions());
        exam.setShowResultPolicy(parse(
                request.showResultPolicy(),
                ShowResultPolicy.class,
                "INVALID_SHOW_RESULT_POLICY"
        ));
        exam.setAutoSubmit(request.autoSubmit());
        exam.setRequireFullscreen(request.requireFullscreen());
        exam.setMaxViolationAllowed(request.maxViolationAllowed());
        exam.setHandleViolation(parse(
                request.handleViolation(),
                HandleViolation.class,
                "INVALID_HANDLE_VIOLATION"
        ));
    }

    private String generateCode() {
        // Ma ngan chi de nhan dien ca thi, khong duoc xem nhu mot secret truy cap.
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase(Locale.ROOT);
            if (!examRepo.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("EXAM_CODE_GENERATION_FAILED");
    }

    private ExamSummaryDTO toSummary(Exam exam) {
        return new ExamSummaryDTO(
                exam.getId(),
                exam.getCode(),
                exam.getTitle(),
                exam.getSubjectId(),
                exam.getSubjectNameSnapshot(),
                exam.getCollectionId(),
                exam.getCollectionNameSnapshot(),
                exam.getStartAt(),
                exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 0,
                exam.getEasyCount() + exam.getMediumCount() + exam.getHardCount(),
                assignmentRepo.countByExamIdAndStatus(exam.getId(), AssignmentStatus.ASSIGNED),
                exam.getStatus(),
                exam.getVersion()
        );
    }

    ExamDetailDTO toDetail(Exam exam) {
        return new ExamDetailDTO(
                exam.getId(),
                exam.getCode(),
                exam.getTitle(),
                exam.getDescription(),
                exam.getSubjectId(),
                exam.getSubjectNameSnapshot(),
                exam.getCollectionId(),
                exam.getCollectionNameSnapshot(),
                exam.getEasyCount(),
                exam.getMediumCount(),
                exam.getHardCount(),
                exam.getStartAt(),
                exam.getEndAt(),
                exam.getDurationMinutes() != null ? exam.getDurationMinutes() : 0,
                exam.getJoinBeforeMinutes(),
                exam.getJoinAfterMinutes(),
                exam.isShuffleQuestions(),
                exam.isShuffleOptions(),
                exam.getShowResultPolicy(),
                exam.isAutoSubmit(),
                exam.isRequireFullscreen(),
                exam.getMaxViolationAllowed(),
                exam.getHandleViolation(),
                assignmentRepo.countByExamIdAndStatus(exam.getId(), AssignmentStatus.ASSIGNED),
                exam.getStatus(),
                exam.getVersion()
        );
    }

    private org.springframework.data.domain.Pageable pageable(
            int page,
            int size,
            String sortValue
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new IllegalArgumentException("INVALID_PAGE_REQUEST");
        }
        String[] parts = sortValue == null || sortValue.isBlank()
                ? new String[]{"updatedAt", "desc"}
                : sortValue.split(",", 2);
        if (!SORT_FIELDS.contains(parts[0])) {
            throw new IllegalArgumentException("INVALID_SORT_FIELD");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 2
                    ? Sort.Direction.fromString(parts[1])
                    : Sort.Direction.ASC;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("INVALID_SORT_DIRECTION");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

    private <T extends Enum<T>> T parse(String value, Class<T> type, String errorCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
