package com.result_service.result_service.service.results;

import com.result_service.result_service.client.AuthServiceClient;
import com.result_service.result_service.client.ExamPaperPoolDTO;
import com.result_service.result_service.client.ExamServiceClient;
import com.result_service.result_service.client.PaperQuestionDTO;
import com.result_service.result_service.client.StudentSummary;
import com.result_service.result_service.client.RuntimeActivationDTO;
import com.result_service.result_service.model.dto.common.PageResponseDTO;
import com.result_service.result_service.model.dto.results.PublishResultsResponseDTO;
import com.result_service.result_service.model.dto.results.ResultReviewStatusDTO;
import com.result_service.result_service.model.dto.results.ResultVisibilityStateDTO;
import com.result_service.result_service.model.dto.results.StudentResultDetailDTO;
import com.result_service.result_service.model.dto.results.StudentResultSummaryDTO;
import com.result_service.result_service.model.dto.results.TeacherExamResultsDTO;
import com.result_service.result_service.model.dto.results.TeacherResultDetailDTO;
import com.result_service.result_service.model.dto.results.TeacherResultRowDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.ResultAnswer;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import com.result_service.result_service.util.exception.ConflictException;
import com.result_service.result_service.util.exception.NotFoundException;
import com.result_service.result_service.util.exception.UnauthorizedException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ResultReviewService {

    private final ExamResultRepo examResultRepo;
    private final ResultAnswerRepo resultAnswerRepo;
    private final ResultSnapshotReader snapshotReader;
    private final ResultPolicyResolver policyResolver;
    private final ResultRankingService rankingService;
    private final AuthServiceClient authServiceClient;
    private final ExamServiceClient examServiceClient;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ResultReviewService(
            ExamResultRepo examResultRepo,
            ResultAnswerRepo resultAnswerRepo,
            ResultSnapshotReader snapshotReader,
            ResultPolicyResolver policyResolver,
            ResultRankingService rankingService,
            AuthServiceClient authServiceClient,
            ExamServiceClient examServiceClient,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examResultRepo = examResultRepo;
        this.resultAnswerRepo = resultAnswerRepo;
        this.snapshotReader = snapshotReader;
        this.policyResolver = policyResolver;
        this.rankingService = rankingService;
        this.authServiceClient = authServiceClient;
        this.examServiceClient = examServiceClient;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    /**
     * Lay danh sach ket qua thi cua mot ca thi phuc vu cho phan he giao vien.
     * Thuc hien phan trang, tinh thu hang cho hoc sinh, phan giai thong tin hoc sinh, tinh toan cac thong ke (diem trung binh, cao nhat, thap nhat, pho diem).
     */
    @Transactional(readOnly = true)
    public TeacherExamResultsDTO teacherExamResults(UUID teacherId, UUID examId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "gradedAt"));
        var resultPage = examResultRepo.findByExamId(examId, pageable);
        List<ExamResult> all = examResultRepo.findByExamId(examId);
        if (all.isEmpty()) {
            throw new NotFoundException("EXAM_RESULTS_NOT_FOUND");
        }
        // Xac thuc giao vien dang yeu cau co dung la nguoi so huu ca thi hay khong
        requireTeacher(teacherId, all.getFirst());

        // Tinh toan bang xep hang cho tat ca thi sinh trong ca thi
        Map<UUID, Integer> ranks = rankingService.ranks(all);
        
        // Goi API cua auth-service de phan giai ma hoc sinh va ten hoc sinh cho cac ban ghi hien thi tren trang hien tai
        Map<UUID, StudentSummary> resolvedStudents = resolveStudents(resultPage.getContent());
        List<TeacherResultRowDTO> rows = resultPage.getContent().stream()
                .map(result -> teacherRow(result, ranks, resolvedStudents))
                .toList();
                
        PageResponseDTO<TeacherResultRowDTO> pageDto = PageResponseDTO.from(new PageImpl<>(
                rows,
                resultPage.getPageable(),
                resultPage.getTotalElements()
        ));
        
        // Lay thong tin ky thi tu snapshot hoac fallback tu API
        ExamSnapshotInfo exam = resolvedExam(all.getFirst());
        return new TeacherExamResultsDTO(
                examId,
                exam.code(),
                title(exam),
                exam.subjectName(),
                exam.showResultPolicy(),
                exam.startAt(),
                exam.endAt(),
                all.size(),
                all.size(),
                all.stream().filter(result -> reviewStatus(result) == ResultReviewStatus.RELEASED).count(),
                all.stream().filter(result -> reviewStatus(result) == ResultReviewStatus.PENDING_REVIEW).count(),
                average(all),
                all.stream().map(rankingService::effectiveScore).max(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                all.stream().map(rankingService::effectiveScore).min(Comparator.naturalOrder()).orElse(BigDecimal.ZERO),
                distribution(all),
                pageDto
        );
    }

    /**
     * Lay chi tiet bai thi cua mot hoc sinh cu the duoi goc nhin cua giao vien (hien thi tat ca cau hoi, dap an dung va lua chon cua hoc sinh).
     */
    @Transactional(readOnly = true)
    public TeacherResultDetailDTO teacherResultDetail(UUID teacherId, UUID examId, UUID resultId) {
        ExamResult result = examResultRepo.findByExamIdAndId(examId, resultId)
                .orElseThrow(() -> new NotFoundException("RESULT_NOT_FOUND"));
        requireTeacher(teacherId, result);
        
        Map<UUID, Integer> ranks = rankingService.ranks(examResultRepo.findByExamId(examId));
        List<ResultAnswer> resultAnswers = resultAnswerRepo.findByResultIdOrderByQuestionOrderAsc(result.getId());
        
        // Neu snapshot cau hoi trong DB bi loi thoi hoac thieu, goi API exam-service lay thong tin cau hoi de fallback
        Map<UUID, PaperQuestionDTO> fallbackQuestions = fallbackQuestions(examId, resultAnswers);
        Map<UUID, StudentSummary> resolvedStudents = resolveStudents(List.of(result));
        
        List<TeacherResultDetailDTO.TeacherResultAnswerDTO> answers = resultAnswers.stream()
                .map(answer -> teacherAnswer(answer, fallbackQuestions))
                .toList();
                
        return new TeacherResultDetailDTO(
                teacherRow(result, ranks, resolvedStudents),
                result.getAdjustmentReason(),
                result.getAdjustedAt(),
                result.getAdjustedBy(),
                answers,
                List.of(),
                List.of()
        );
    }

    /**
     * Chinh sua diem so cua mot hoc sinh boi giao vien.
     * Diem moi phai nam trong khoang [0..maxScore], phai co ly do dieu chinh thich hop.
     */
    @Transactional
    public TeacherResultRowDTO adjustScore(UUID teacherId, UUID examId, UUID resultId, BigDecimal adjustedScore, String reason) {
        ExamResult result = examResultRepo.findByExamIdAndId(examId, resultId)
                .orElseThrow(() -> new NotFoundException("RESULT_NOT_FOUND"));
        requireTeacher(teacherId, result);
        
        if (adjustedScore == null) {
            throw new IllegalArgumentException("ADJUSTED_SCORE_REQUIRED");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("ADJUSTMENT_REASON_REQUIRED");
        }
        if (adjustedScore.compareTo(BigDecimal.ZERO) < 0) {
            throw new ConflictException("ADJUSTED_SCORE_NEGATIVE");
        }
        if (adjustedScore.compareTo(result.getMaxScore()) > 0) {
            throw new ConflictException("ADJUSTED_SCORE_EXCEEDS_MAX_SCORE");
        }
        
        // Luu thong tin dieu chinh diem
        result.setAdjustedScore(adjustedScore.setScale(4, RoundingMode.HALF_UP));
        result.setAdjustmentReason(reason);
        result.setAdjustedAt(OffsetDateTime.now(clock));
        result.setAdjustedBy(teacherId);
        examResultRepo.save(result);
        
        return teacherRow(result, rankingService.ranks(examResultRepo.findByExamId(examId)), resolveStudents(List.of(result)));
    }

    /**
     * Giao vien phe duyet (publish) diem so cho hoc sinh cua ca thi.
     * Neu resultIds null/empty, he thong se thuc hien publish cho tat ca hoc sinh trong ca thi.
     */
    @Transactional
    public PublishResultsResponseDTO publish(UUID teacherId, UUID examId, List<UUID> resultIds) {
        List<ExamResult> all = examResultRepo.findByExamId(examId);
        if (all.isEmpty()) {
            throw new NotFoundException("EXAM_RESULTS_NOT_FOUND");
        }
        requireTeacher(teacherId, all.getFirst());
        
        List<ExamResult> targets = resultIds == null || resultIds.isEmpty()
                ? all
                : all.stream().filter(result -> resultIds.contains(result.getId())).toList();
                
        int published = 0;
        int unchanged = 0;
        OffsetDateTime now = OffsetDateTime.now(clock);
        
        for (ExamResult result : targets) {
            // Neu da duoc publish tu truoc do thi bo qua
            if (reviewStatus(result) == ResultReviewStatus.RELEASED) {
                unchanged++;
                continue;
            }
            result.setReviewStatus(ResultReviewStatus.RELEASED);
            result.setReleasedAt(now);
            result.setReleasedBy(teacherId);
            published++;
        }
        examResultRepo.saveAll(targets);
        return new PublishResultsResponseDTO(targets.size(), published, unchanged);
    }

    /**
     * Lay danh sach tom tat cac ket qua thi cua mot hoc sinh.
     */
    @Transactional(readOnly = true)
    public List<StudentResultSummaryDTO> studentResults(UUID studentId) {
        List<ExamResult> results = examResultRepo.findByStudentIdOrderByGradedAtDesc(studentId);
        return results.stream()
                .map(result -> studentSummary(result, rankingService.ranks(examResultRepo.findByExamId(result.getExamId()))))
                .toList();
    }

    /**
     * Lay chi tiet ket qua thi cua hoc sinh cho mot ky thi.
     * Neu ket qua chua duoc phep hien thi cho hoc sinh (chua den gio close, hoac chua duoc giao vien publish),
     * tra ve cac gia tri diem so va chi tiet cau hoi bang NULL de bao mat.
     */
    @Transactional(readOnly = true)
    public StudentResultDetailDTO studentResult(UUID studentId, UUID examId) {
        ExamResult result = examResultRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new NotFoundException("RESULT_NOT_FOUND"));
        Map<UUID, Integer> ranks = rankingService.ranks(examResultRepo.findByExamId(examId));
        StudentResultSummaryDTO summary = studentSummary(result, ranks);
        
        // Neu chinh sach chan khong cho phep hoc sinh xem diem hien tai, giau thong tin diem di
        if (!policyResolver.visibleToStudent(result)) {
            return new StudentResultDetailDTO(
                    result.getExamId(),
                    summary.code(),
                    summary.title(),
                    summary.subjectName(),
                    summary.visibilityState(),
                    summary.message(),
                    summary.availableAt(),
                    null, // Che diem so
                    null, // Che diem toi da
                    null, // Che phan tram
                    null, // Che thu hang
                    ranks.size(),
                    0,
                    0,
                    0,
                    0,
                    result.getSubmittedAt(),
                    result.getGradedAt(),
                    result.getReleasedAt()
            );
        }
        
        // Neu thoa man cac chinh sach hien thi, tra ve diem va thong tin chi tiet
        return new StudentResultDetailDTO(
                result.getExamId(),
                summary.code(),
                summary.title(),
                summary.subjectName(),
                summary.visibilityState(),
                summary.message(),
                summary.availableAt(),
                summary.score(),
                summary.maxScore(),
                summary.percentage(),
                summary.rank(),
                ranks.size(),
                result.getTotalQuestions(),
                result.getCorrectCount(),
                result.getWrongCount(),
                result.getBlankCount(),
                result.getSubmittedAt(),
                result.getGradedAt(),
                result.getReleasedAt()
        );
    }

    /**
     * Xuat file Excel ket qua thi cua ca thi de giao vien tai ve.
     */
    @Transactional(readOnly = true)
    public byte[] exportExamResults(UUID teacherId, UUID examId) {
        List<ExamResult> results = examResultRepo.findByExamId(examId);
        if (results.isEmpty()) {
            throw new NotFoundException("EXAM_RESULTS_NOT_FOUND");
        }
        requireTeacher(teacherId, results.getFirst());
        Map<UUID, Integer> ranks = rankingService.ranks(results);
        Map<UUID, StudentSummary> resolvedStudents = resolveStudents(results);
        
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet("results");
            String[] headers = {
                    "studentCode", "studentName", "originalScore", "adjustedScore", "effectiveScore",
                    "maxScore", "percentage", "rank", "correct", "wrong", "blank", "reviewStatus",
                    "submittedAt", "gradedAt", "releasedAt"
            };
            Row header = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                header.createCell(i).setCellValue(headers[i]);
            }
            int rowIndex = 1;
            for (ExamResult result : results) {
                TeacherResultRowDTO rowDto = teacherRow(result, ranks, resolvedStudents);
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(nullToEmpty(rowDto.studentCode()));
                row.createCell(1).setCellValue(nullToEmpty(rowDto.studentName()));
                row.createCell(2).setCellValue(rowDto.originalScore().doubleValue());
                row.createCell(3).setCellValue(rowDto.adjustedScore() != null ? rowDto.adjustedScore().doubleValue() : 0);
                row.createCell(4).setCellValue(rowDto.effectiveScore().doubleValue());
                row.createCell(5).setCellValue(rowDto.maxScore().doubleValue());
                row.createCell(6).setCellValue(rowDto.percentage().doubleValue());
                row.createCell(7).setCellValue(rowDto.rank());
                row.createCell(8).setCellValue(rowDto.correctCount());
                row.createCell(9).setCellValue(rowDto.wrongCount());
                row.createCell(10).setCellValue(rowDto.blankCount());
                row.createCell(11).setCellValue(rowDto.reviewStatus().name());
                row.createCell(12).setCellValue(string(rowDto.submittedAt()));
                row.createCell(13).setCellValue(string(rowDto.gradedAt()));
                row.createCell(14).setCellValue(string(rowDto.releasedAt()));
            }
            workbook.write(output);
            return output.toByteArray();
        } catch (Exception exception) {
            throw new IllegalStateException("RESULT_EXPORT_FAILED", exception);
        }
    }

    private TeacherResultRowDTO teacherRow(
            ExamResult result,
            Map<UUID, Integer> ranks,
            Map<UUID, StudentSummary> resolvedStudents
    ) {
        StudentSnapshotInfo student = resolvedStudent(result, resolvedStudents);
        ResultVisibilityStateDTO visibility = policyResolver.visibility(result);
        return new TeacherResultRowDTO(
                result.getId(),
                result.getExamId(),
                result.getStudentId(),
                student.studentCode(),
                student.studentName(),
                result.getTotalScore(),
                result.getAdjustedScore(),
                rankingService.effectiveScore(result),
                result.getMaxScore(),
                percentage(result),
                ranks.getOrDefault(result.getId(), 0),
                result.getTotalQuestions(),
                result.getCorrectCount(),
                result.getWrongCount(),
                result.getBlankCount(),
                ResultReviewStatusDTO.valueOf(reviewStatus(result).name()),
                visibility,
                result.getSubmittedAt(),
                result.getGradedAt(),
                result.getReleasedAt()
        );
    }

    private StudentResultSummaryDTO studentSummary(ExamResult result, Map<UUID, Integer> ranks) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        ResultVisibilityStateDTO state = policyResolver.visibility(result);
        boolean visible = policyResolver.visibleToStudent(result);
        return new StudentResultSummaryDTO(
                result.getExamId(),
                exam.code(),
                title(exam),
                exam.subjectName(),
                state,
                policyResolver.message(state),
                policyResolver.availableAt(result),
                visible ? rankingService.effectiveScore(result) : null,
                visible ? result.getMaxScore() : null,
                visible ? percentage(result) : null,
                visible ? ranks.get(result.getId()) : null,
                ranks.size(),
                result.getSubmittedAt(),
                result.getGradedAt(),
                result.getReleasedAt()
        );
    }

    private Map<UUID, StudentSummary> resolveStudents(List<ExamResult> results) {
        return authServiceClient.resolveStudents(results.stream()
                .map(ExamResult::getStudentId)
                .filter(id -> id != null)
                .distinct()
                .toList());
    }

    private StudentSnapshotInfo resolvedStudent(ExamResult result, Map<UUID, StudentSummary> resolvedStudents) {
        StudentSnapshotInfo snapshot = snapshotReader.student(result);
        StudentSummary resolved = resolvedStudents.get(result.getStudentId());
        if (resolved == null) {
            return snapshot;
        }
        String code = firstNonBlank(snapshot.studentCode(), resolved.studentCode());
        String snapshotName = result.getStudentId().toString().equals(snapshot.studentName()) ? null : snapshot.studentName();
        String name = firstNonBlank(snapshotName, resolved.fullName(), resolved.displayName());
        return new StudentSnapshotInfo(
                result.getStudentId(),
                code,
                firstNonBlank(name, result.getStudentId().toString())
        );
    }

    private ExamSnapshotInfo resolvedExam(ExamResult result) {
        ExamSnapshotInfo snapshot = snapshotReader.exam(result);
        if (!needsExamFallback(snapshot)) {
            return snapshot;
        }
        RuntimeActivationDTO activation = examServiceClient.getRuntimeActivation(result.getExamId()).orElse(null);
        if (activation == null) {
            return snapshot;
        }
        return new ExamSnapshotInfo(
                firstNonNull(snapshot.examId(), activation.examId(), result.getExamId()),
                snapshot.snapshotVersion() > 0 ? snapshot.snapshotVersion() : activation.snapshotVersion(),
                firstNonBlank(snapshot.code(), activation.code()),
                firstNonBlank(snapshot.title(), activation.title()),
                firstNonNull(snapshot.subjectId(), activation.subjectId(), null),
                firstNonBlank(snapshot.subjectName(), activation.subjectName()),
                firstNonNull(snapshot.ownerTeacherId(), activation.ownerTeacherId(), null),
                firstNonNull(snapshot.startAt(), activation.startAt(), null),
                firstNonNull(snapshot.endAt(), activation.endAt(), null),
                firstNonBlank(snapshot.showResultPolicy(), activation.showResultPolicy())
        );
    }

    private boolean needsExamFallback(ExamSnapshotInfo snapshot) {
        return snapshot.ownerTeacherId() == null
                || snapshot.title() == null
                || snapshot.subjectName() == null
                || snapshot.showResultPolicy() == null;
    }

    private Map<UUID, PaperQuestionDTO> fallbackQuestions(UUID examId, List<ResultAnswer> answers) {
        boolean needsFallback = answers.stream().anyMatch(this::needsQuestionFallback);
        if (!needsFallback) {
            return Map.of();
        }
        ExamPaperPoolDTO pool = examServiceClient.getPaperPool(examId).orElse(null);
        if (pool == null || pool.questions() == null) {
            return Map.of();
        }
        return pool.questions().stream()
                .filter(question -> question != null && question.questionId() != null)
                .collect(Collectors.toMap(PaperQuestionDTO::questionId, Function.identity(), (left, right) -> left));
    }

    private boolean needsQuestionFallback(ResultAnswer answer) {
        JsonNode snapshot = questionSnapshot(answer);
        return text(snapshot, "content") == null || !snapshot.path("options").isArray();
    }

    private TeacherResultDetailDTO.TeacherResultAnswerDTO teacherAnswer(
            ResultAnswer answer,
            Map<UUID, PaperQuestionDTO> fallbackQuestions
    ) {
        List<UUID> selectedOptionIds = readUuidList(answer.getSelectedOptionIds());
        List<UUID> correctOptionIds = readUuidList(answer.getCorrectOptionIds());
        JsonNode snapshot = questionSnapshot(answer);
        PaperQuestionDTO fallback = fallbackQuestions.get(answer.getQuestionId());
        TeacherResultDetailDTO.QuestionDisplayDTO question = questionDisplay(answer.getQuestionId(), snapshot, fallback);
        List<TeacherResultDetailDTO.OptionDisplayDTO> options = optionDisplays(snapshot, fallback, selectedOptionIds, correctOptionIds);
        return new TeacherResultDetailDTO.TeacherResultAnswerDTO(
                answer.getQuestionId(),
                answer.getQuestionOrder(),
                selectedOptionIds,
                correctOptionIds,
                answer.isCorrect(),
                answer.getScoreAwarded(),
                answer.getMaxScore(),
                answer.getGradingNote(),
                snapshotReader.object(answer.getQuestionSnapshot()),
                answerState(answer, selectedOptionIds),
                question,
                options
        );
    }

    private JsonNode questionSnapshot(ResultAnswer answer) {
        try {
            String json = answer.getQuestionSnapshot();
            if (json == null || json.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private TeacherResultDetailDTO.QuestionDisplayDTO questionDisplay(
            UUID questionId,
            JsonNode snapshot,
            PaperQuestionDTO fallback
    ) {
        return new TeacherResultDetailDTO.QuestionDisplayDTO(
                questionId,
                firstNonBlank(text(snapshot, "content"), fallback != null ? fallback.content() : null),
                firstNonBlank(text(snapshot, "type"), fallback != null ? fallback.type() : null),
                firstNonBlank(text(snapshot, "difficulty"), fallback != null ? fallback.difficulty() : null),
                firstNonBlank(text(snapshot, "contentFormat"), fallback != null ? fallback.contentFormat() : null)
        );
    }

    private List<TeacherResultDetailDTO.OptionDisplayDTO> optionDisplays(
            JsonNode snapshot,
            PaperQuestionDTO fallback,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds
    ) {
        List<JsonNode> optionNodes = new ArrayList<>();
        if (snapshot.path("options").isArray()) {
            snapshot.path("options").forEach(optionNodes::add);
        }
        if (!optionNodes.isEmpty()) {
            return optionNodes.stream()
                    .map(option -> optionDisplay(
                            uuid(option, "optionId"),
                            text(option, "key"),
                            text(option, "content"),
                            text(option, "contentFormat"),
                            selectedOptionIds,
                            correctOptionIds
                    ))
                    .toList();
        }
        if (fallback == null || fallback.options() == null) {
            return List.of();
        }
        return fallback.options().stream()
                .map(option -> new TeacherResultDetailDTO.OptionDisplayDTO(
                        option.optionId(),
                        option.key(),
                        option.content(),
                        option.contentFormat(),
                        selectedOptionIds.contains(option.optionId()),
                        correctOptionIds.contains(option.optionId())
                ))
                .toList();
    }

    private TeacherResultDetailDTO.OptionDisplayDTO optionDisplay(
            UUID optionId,
            String key,
            String content,
            String contentFormat,
            List<UUID> selectedOptionIds,
            List<UUID> correctOptionIds
    ) {
        return new TeacherResultDetailDTO.OptionDisplayDTO(
                optionId,
                key,
                content,
                contentFormat,
                selectedOptionIds.contains(optionId),
                correctOptionIds.contains(optionId)
        );
    }

    private String answerState(ResultAnswer answer, List<UUID> selectedOptionIds) {
        if (selectedOptionIds.isEmpty() || "BLANK".equalsIgnoreCase(answer.getGradingNote())) {
            return "BLANK";
        }
        return answer.isCorrect() ? "CORRECT" : "WRONG";
    }

    private List<UUID> readUuidList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private void requireTeacher(UUID teacherId, ExamResult result) {
        ExamSnapshotInfo exam = resolvedExam(result);
        if (exam.ownerTeacherId() == null || !exam.ownerTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("RESULT_TEACHER_NOT_ALLOWED");
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception exception) {
            return null;
        }
    }

    @SafeVarargs
    private final <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private ResultReviewStatus reviewStatus(ExamResult result) {
        return result.getReviewStatus() != null ? result.getReviewStatus() : ResultReviewStatus.PENDING_REVIEW;
    }

    private BigDecimal average(List<ExamResult> results) {
        if (results.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = results.stream()
                .map(rankingService::effectiveScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.divide(BigDecimal.valueOf(results.size()), 4, RoundingMode.HALF_UP);
    }

    private BigDecimal percentage(ExamResult result) {
        if (result.getMaxScore().compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return rankingService.effectiveScore(result)
                .multiply(BigDecimal.valueOf(100))
                .divide(result.getMaxScore(), 3, RoundingMode.HALF_UP);
    }

    private List<TeacherExamResultsDTO.ScoreBucketDTO> distribution(List<ExamResult> results) {
        long[] buckets = new long[5];
        for (ExamResult result : results) {
            int index = percentage(result).divide(BigDecimal.valueOf(20), 0, RoundingMode.DOWN).intValue();
            buckets[Math.min(index, 4)]++;
        }
        List<TeacherExamResultsDTO.ScoreBucketDTO> response = new ArrayList<>();
        response.add(new TeacherExamResultsDTO.ScoreBucketDTO("0-20%", buckets[0]));
        response.add(new TeacherExamResultsDTO.ScoreBucketDTO("21-40%", buckets[1]));
        response.add(new TeacherExamResultsDTO.ScoreBucketDTO("41-60%", buckets[2]));
        response.add(new TeacherExamResultsDTO.ScoreBucketDTO("61-80%", buckets[3]));
        response.add(new TeacherExamResultsDTO.ScoreBucketDTO("81-100%", buckets[4]));
        return response;
    }

    private String title(ExamSnapshotInfo exam) {
        return exam.title() != null ? exam.title() : "Ca thi";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String string(OffsetDateTime value) {
        return value == null ? "" : value.toString();
    }
}
