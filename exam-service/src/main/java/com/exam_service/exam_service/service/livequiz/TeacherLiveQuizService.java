package com.exam_service.exam_service.service.livequiz;

import com.exam_service.exam_service.client.QuestionCollectionMetadata;
import com.exam_service.exam_service.client.QuestionServiceClient;
import com.exam_service.exam_service.client.RuntimeLiveQuizClient;
import com.exam_service.exam_service.model.dto.common.PageResponseDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizDetailDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizRequestDTO;
import com.exam_service.exam_service.model.dto.livequiz.LiveQuizSummaryDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.model.entity.enums.LiveQuizJoinPolicy;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
/**
 * Quan ly cau hinh live quiz rieng voi scheduled exam.
 *
 * Service nay chi lam viec voi ban nhap live quiz: title, collection va cac tuy
 * chon hien thi. Cac cot schedule/assignment cua exam thuong khong duoc set tai
 * day de tranh lam lech logic tao ca thi exam co lich.
 */
public class TeacherLiveQuizService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "title");

    private final ExamRepo examRepo;
    private final ExamQuestionRepo questionRepo;
    private final QuestionServiceClient questionServiceClient;
    private final RuntimeLiveQuizClient runtimeLiveQuizClient;

    public TeacherLiveQuizService(
            ExamRepo examRepo,
            ExamQuestionRepo questionRepo,
            QuestionServiceClient questionServiceClient,
            RuntimeLiveQuizClient runtimeLiveQuizClient
    ) {
        this.examRepo = examRepo;
        this.questionRepo = questionRepo;
        this.questionServiceClient = questionServiceClient;
        this.runtimeLiveQuizClient = runtimeLiveQuizClient;
    }

    public LiveQuizDetailDTO create(UUID subjectId, UUID teacherId, LiveQuizRequestDTO request) {
        // Chi validate metadata collection khi tao ban nhap; cau hoi that duoc dong bang luc prepare.
        QuestionCollectionMetadata metadata = validateCollection(subjectId, teacherId, request.collectionId());
        Exam quiz = new Exam();
        quiz.setCode(generateCode());
        quiz.setSubjectId(subjectId);
        quiz.setCreatedByTeacherId(teacherId);
        quiz.setStatus(ExamStatus.DRAFT);
        quiz.setExamType(ExamType.LIVE_QUIZ);
        apply(quiz, request, metadata);
        return toDetail(examRepo.save(quiz));
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<LiveQuizSummaryDTO> list(
            UUID subjectId,
            UUID teacherId,
            String status,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        // Scope list luon kem subject + teacher + LIVE_QUIZ de scheduled exam khong bi lo vao API nay.
        Specification<Exam> specification = liveQuizScope(subjectId, teacherId);
        if (status != null && !status.isBlank()) {
            ExamStatus parsed = parse(status, ExamStatus.class, "INVALID_EXAM_STATUS");
            specification = specification.and((root, query, cb) -> cb.equal(root.get("status"), parsed));
        }
        if (keyword != null && !keyword.isBlank()) {
            String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("code")), pattern)
            ));
        }
        Page<Exam> quizzes = examRepo.findAll(specification, pageable(page, size, sort));
        Map<UUID, RuntimeLiveQuizClient.LiveQuizRoomLookupResponse> roomsByExamId = latestRoomsByExamId(quizzes);
        Page<LiveQuizSummaryDTO> result = quizzes.map(quiz -> toSummary(quiz, roomsByExamId.get(quiz.getId())));
        return PageResponseDTO.from(result);
    }

    @Transactional(readOnly = true)
    public LiveQuizDetailDTO get(UUID subjectId, UUID quizId, UUID teacherId) {
        return toDetail(requireOwnedLiveQuiz(subjectId, quizId, teacherId));
    }

    public LiveQuizDetailDTO update(
            UUID subjectId,
            UUID quizId,
            UUID teacherId,
            LiveQuizRequestDTO request
    ) {
        // Chi cho sua DRAFT. Khi da PREPARED, snapshot cau hoi va room co the da duoc tao.
        Exam quiz = requireEditableLiveQuiz(subjectId, quizId, teacherId);
        QuestionCollectionMetadata metadata = validateCollection(subjectId, teacherId, request.collectionId());
        apply(quiz, request, metadata);
        return toDetail(examRepo.save(quiz));
    }

    Exam requireOwnedLiveQuiz(UUID subjectId, UUID quizId, UUID teacherId) {
        Exam quiz = examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(quizId, subjectId, teacherId)
                .orElseThrow(() -> new NotFoundException("LIVE_QUIZ_NOT_FOUND"));
        // Tra ve NOT_FOUND cho sai type de client khong the dung live quiz API de doc exam thuong.
        if (quiz.getExamType() != ExamType.LIVE_QUIZ) {
            throw new NotFoundException("LIVE_QUIZ_NOT_FOUND");
        }
        return quiz;
    }

    private Exam requireEditableLiveQuiz(UUID subjectId, UUID quizId, UUID teacherId) {
        Exam quiz = requireOwnedLiveQuiz(subjectId, quizId, teacherId);
        if (quiz.getStatus() != ExamStatus.DRAFT) {
            throw new ConflictException("LIVE_QUIZ_NOT_EDITABLE");
        }
        return quiz;
    }

    private QuestionCollectionMetadata validateCollection(UUID subjectId, UUID teacherId, UUID collectionId) {
        // Question Service la nguon su that ve collection va quyen so huu cua giao vien.
        QuestionCollectionMetadata metadata = questionServiceClient.getExamMetadata(subjectId, collectionId, teacherId);
        if (!subjectId.equals(metadata.subjectId())) {
            throw new IllegalArgumentException("COLLECTION_SUBJECT_MISMATCH");
        }
        if (metadata.easy() + metadata.medium() + metadata.hard() <= 0) {
            throw new IllegalArgumentException("LIVE_QUIZ_COLLECTION_EMPTY");
        }
        return metadata;
    }

    private void apply(Exam quiz, LiveQuizRequestDTO request, QuestionCollectionMetadata metadata) {
        quiz.setTitle(request.title().trim());
        quiz.setDescription(trimToNull(request.description()));
        quiz.setCollectionId(metadata.collectionId());
        quiz.setCollectionNameSnapshot(metadata.name());
        quiz.setSubjectNameSnapshot(metadata.subjectName());
        // Live quiz lay toan bo collection; cac cot count chi la snapshot de hien thi truoc prepare.
        // Viec chon cau hoi khong dung quota easy/medium/hard nhu scheduled exam.
        quiz.setEasyCount(Math.toIntExact(metadata.easy()));
        quiz.setMediumCount(Math.toIntExact(metadata.medium()));
        quiz.setHardCount(Math.toIntExact(metadata.hard()));
        boolean shuffleQuestions = request.shuffleQuestions() == null || request.shuffleQuestions();
        quiz.setShuffleQuestions(shuffleQuestions);
        quiz.setShuffleOptions(false);
        quiz.setLiveQuizShuffleQuestions(shuffleQuestions);
        quiz.setLiveQuizShowLeaderboard(request.showLeaderboard() == null || request.showLeaderboard());
        quiz.setLiveQuizShowCorrectAnswer(Boolean.TRUE.equals(request.showCorrectAnswer()));
        quiz.setLiveQuizJoinPolicy(parseJoinPolicy(request.joinPolicy()));
    }

    LiveQuizDetailDTO toDetail(Exam quiz) {
        return new LiveQuizDetailDTO(
                quiz.getId(),
                quiz.getCode(),
                quiz.getTitle(),
                quiz.getDescription(),
                quiz.getSubjectId(),
                quiz.getSubjectNameSnapshot(),
                quiz.getCollectionId(),
                quiz.getCollectionNameSnapshot(),
                questionCount(quiz),
                liveQuizShuffle(quiz),
                Boolean.TRUE.equals(quiz.getLiveQuizShowLeaderboard()),
                Boolean.TRUE.equals(quiz.getLiveQuizShowCorrectAnswer()),
                quiz.getLiveQuizJoinPolicy() == null ? LiveQuizJoinPolicy.CODE_ONLY : quiz.getLiveQuizJoinPolicy(),
                quiz.getStatus(),
                quiz.getSnapshotVersion(),
                quiz.getScheduledAt(),
                quiz.getVersion()
        );
    }

    private LiveQuizSummaryDTO toSummary(
            Exam quiz,
            RuntimeLiveQuizClient.LiveQuizRoomLookupResponse room
    ) {
        return new LiveQuizSummaryDTO(
                quiz.getId(),
                quiz.getCode(),
                quiz.getTitle(),
                quiz.getSubjectId(),
                quiz.getSubjectNameSnapshot(),
                quiz.getCollectionId(),
                quiz.getCollectionNameSnapshot(),
                questionCount(quiz),
                liveQuizShuffle(quiz),
                Boolean.TRUE.equals(quiz.getLiveQuizShowLeaderboard()),
                Boolean.TRUE.equals(quiz.getLiveQuizShowCorrectAnswer()),
                quiz.getLiveQuizJoinPolicy() == null ? LiveQuizJoinPolicy.CODE_ONLY : quiz.getLiveQuizJoinPolicy(),
                quiz.getStatus(),
                quiz.getSnapshotVersion(),
                quiz.getVersion(),
                room == null ? null : room.roomId(),
                room == null ? null : room.roomCode(),
                room == null ? null : room.status()
        );
    }

    private Map<UUID, RuntimeLiveQuizClient.LiveQuizRoomLookupResponse> latestRoomsByExamId(Page<Exam> quizzes) {
        // Chi PREPARED moi co room runtime. DRAFT khong can lookup de tranh goi service thua.
        List<UUID> preparedQuizIds = quizzes.getContent().stream()
                .filter(quiz -> quiz.getStatus() == ExamStatus.PREPARED)
                .map(Exam::getId)
                .toList();
        return runtimeLiveQuizClient.latestRooms(preparedQuizIds).stream()
                .collect(Collectors.toMap(
                        RuntimeLiveQuizClient.LiveQuizRoomLookupResponse::examId,
                        Function.identity(),
                        (first, ignored) -> first
                ));
    }

    private int questionCount(Exam quiz) {
        // Sau prepare, uu tien dem snapshot da dong bang de UI thay dung so cau se choi.
        if (quiz.getStatus() == ExamStatus.PREPARED && questionRepo.existsByExamId(quiz.getId())) {
            return questionRepo.findAllByExamIdOrderBySortOrderAsc(quiz.getId()).size();
        }
        // Truoc prepare, dung metadata collection da snapshot tren ban nhap.
        return quiz.getEasyCount() + quiz.getMediumCount() + quiz.getHardCount();
    }

    private boolean liveQuizShuffle(Exam quiz) {
        return quiz.getLiveQuizShuffleQuestions() == null || quiz.getLiveQuizShuffleQuestions();
    }

    private Specification<Exam> liveQuizScope(UUID subjectId, UUID teacherId) {
        return (root, query, cb) -> cb.and(
                cb.equal(root.get("subjectId"), subjectId),
                cb.equal(root.get("createdByTeacherId"), teacherId),
                cb.equal(root.get("examType"), ExamType.LIVE_QUIZ)
        );
    }

    private org.springframework.data.domain.Pageable pageable(int page, int size, String sortValue) {
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
            direction = parts.length == 2 ? Sort.Direction.fromString(parts[1]) : Sort.Direction.ASC;
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("INVALID_SORT_DIRECTION");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

    private LiveQuizJoinPolicy parseJoinPolicy(String value) {
        if (value == null || value.isBlank()) {
            // Phase nay chi ho tro hoc sinh vao phong bang ma phong.
            return LiveQuizJoinPolicy.CODE_ONLY;
        }
        return parse(value, LiveQuizJoinPolicy.class, "INVALID_LIVE_QUIZ_JOIN_POLICY");
    }

    private <T extends Enum<T>> T parse(String value, Class<T> type, String errorCode) {
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException(errorCode);
        }
    }

    private String generateCode() {
        for (int attempt = 0; attempt < 5; attempt++) {
            String code = UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
            if (!examRepo.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalStateException("LIVE_QUIZ_CODE_GENERATION_FAILED");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
