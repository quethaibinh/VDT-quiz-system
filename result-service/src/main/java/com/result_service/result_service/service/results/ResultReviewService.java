package com.result_service.result_service.service.results;

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

@Service
public class ResultReviewService {

    private final ExamResultRepo examResultRepo;
    private final ResultAnswerRepo resultAnswerRepo;
    private final ResultSnapshotReader snapshotReader;
    private final ResultPolicyResolver policyResolver;
    private final ResultRankingService rankingService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public ResultReviewService(
            ExamResultRepo examResultRepo,
            ResultAnswerRepo resultAnswerRepo,
            ResultSnapshotReader snapshotReader,
            ResultPolicyResolver policyResolver,
            ResultRankingService rankingService,
            ObjectMapper objectMapper,
            Clock clock
    ) {
        this.examResultRepo = examResultRepo;
        this.resultAnswerRepo = resultAnswerRepo;
        this.snapshotReader = snapshotReader;
        this.policyResolver = policyResolver;
        this.rankingService = rankingService;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public TeacherExamResultsDTO teacherExamResults(UUID teacherId, UUID examId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "gradedAt"));
        var resultPage = examResultRepo.findByExamId(examId, pageable);
        List<ExamResult> all = examResultRepo.findByExamId(examId);
        if (all.isEmpty()) {
            throw new NotFoundException("EXAM_RESULTS_NOT_FOUND");
        }
        requireTeacher(teacherId, all.getFirst());

        Map<UUID, Integer> ranks = rankingService.ranks(all);
        List<TeacherResultRowDTO> rows = resultPage.getContent().stream()
                .map(result -> teacherRow(result, ranks))
                .toList();
        PageResponseDTO<TeacherResultRowDTO> pageDto = PageResponseDTO.from(new PageImpl<>(
                rows,
                resultPage.getPageable(),
                resultPage.getTotalElements()
        ));
        ExamSnapshotInfo exam = snapshotReader.exam(all.getFirst());
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

    @Transactional(readOnly = true)
    public TeacherResultDetailDTO teacherResultDetail(UUID teacherId, UUID examId, UUID resultId) {
        ExamResult result = examResultRepo.findByExamIdAndId(examId, resultId)
                .orElseThrow(() -> new NotFoundException("RESULT_NOT_FOUND"));
        requireTeacher(teacherId, result);
        Map<UUID, Integer> ranks = rankingService.ranks(examResultRepo.findByExamId(examId));
        List<TeacherResultDetailDTO.TeacherResultAnswerDTO> answers = resultAnswerRepo
                .findByResultIdOrderByQuestionOrderAsc(result.getId())
                .stream()
                .map(this::teacherAnswer)
                .toList();
        return new TeacherResultDetailDTO(
                teacherRow(result, ranks),
                result.getAdjustmentReason(),
                result.getAdjustedAt(),
                result.getAdjustedBy(),
                answers,
                List.of(),
                List.of()
        );
    }

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
        result.setAdjustedScore(adjustedScore.setScale(4, RoundingMode.HALF_UP));
        result.setAdjustmentReason(reason);
        result.setAdjustedAt(OffsetDateTime.now(clock));
        result.setAdjustedBy(teacherId);
        examResultRepo.save(result);
        return teacherRow(result, rankingService.ranks(examResultRepo.findByExamId(examId)));
    }

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

    @Transactional(readOnly = true)
    public List<StudentResultSummaryDTO> studentResults(UUID studentId) {
        List<ExamResult> results = examResultRepo.findByStudentIdOrderByGradedAtDesc(studentId);
        return results.stream()
                .map(result -> studentSummary(result, rankingService.ranks(examResultRepo.findByExamId(result.getExamId()))))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResultDetailDTO studentResult(UUID studentId, UUID examId) {
        ExamResult result = examResultRepo.findByExamIdAndStudentId(examId, studentId)
                .orElseThrow(() -> new NotFoundException("RESULT_NOT_FOUND"));
        Map<UUID, Integer> ranks = rankingService.ranks(examResultRepo.findByExamId(examId));
        StudentResultSummaryDTO summary = studentSummary(result, ranks);
        if (!policyResolver.visibleToStudent(result)) {
            return new StudentResultDetailDTO(
                    result.getExamId(),
                    summary.code(),
                    summary.title(),
                    summary.subjectName(),
                    summary.visibilityState(),
                    summary.message(),
                    summary.availableAt(),
                    null,
                    null,
                    null,
                    null,
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

    @Transactional(readOnly = true)
    public byte[] exportExamResults(UUID teacherId, UUID examId) {
        List<ExamResult> results = examResultRepo.findByExamId(examId);
        if (results.isEmpty()) {
            throw new NotFoundException("EXAM_RESULTS_NOT_FOUND");
        }
        requireTeacher(teacherId, results.getFirst());
        Map<UUID, Integer> ranks = rankingService.ranks(results);
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
                TeacherResultRowDTO rowDto = teacherRow(result, ranks);
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

    private TeacherResultRowDTO teacherRow(ExamResult result, Map<UUID, Integer> ranks) {
        StudentSnapshotInfo student = snapshotReader.student(result);
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

    private TeacherResultDetailDTO.TeacherResultAnswerDTO teacherAnswer(ResultAnswer answer) {
        return new TeacherResultDetailDTO.TeacherResultAnswerDTO(
                answer.getQuestionId(),
                answer.getQuestionOrder(),
                readUuidList(answer.getSelectedOptionIds()),
                readUuidList(answer.getCorrectOptionIds()),
                answer.isCorrect(),
                answer.getScoreAwarded(),
                answer.getMaxScore(),
                answer.getGradingNote(),
                snapshotReader.object(answer.getQuestionSnapshot())
        );
    }

    private List<UUID> readUuidList(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<UUID>>() {});
        } catch (Exception exception) {
            return List.of();
        }
    }

    private void requireTeacher(UUID teacherId, ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        if (exam.ownerTeacherId() == null || !exam.ownerTeacherId().equals(teacherId)) {
            throw new UnauthorizedException("RESULT_TEACHER_NOT_ALLOWED");
        }
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
