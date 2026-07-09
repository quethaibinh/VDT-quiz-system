package com.result_service.result_service.service.results;

import com.result_service.result_service.model.dto.results.ResultVisibilityStateDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.enums.ExamStatus;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import com.result_service.result_service.repository.ExamResultRepo;
import com.result_service.result_service.repository.ResultAnswerRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class ResultReviewServiceTests {

    @Autowired
    private ResultReviewService resultReviewService;

    @Autowired
    private ExamResultRepo examResultRepo;

    @Autowired
    private ResultAnswerRepo resultAnswerRepo;

    @Autowired
    private ObjectMapper objectMapper;

    private UUID teacherId;
    private UUID studentId;
    private UUID examId;
    private UUID subjectId;

    @BeforeEach
    void setUp() {
        resultAnswerRepo.deleteAll();
        examResultRepo.deleteAll();
        teacherId = UUID.randomUUID();
        studentId = UUID.randomUUID();
        examId = UUID.randomUUID();
        subjectId = UUID.randomUUID();
    }

    @Test
    void hidesManualReleaseResultUntilTeacherPublishes() throws Exception {
        ExamResult result = saveResult("NEVER", ResultReviewStatus.PENDING_REVIEW, OffsetDateTime.now().minusHours(1));

        var hidden = resultReviewService.studentResult(studentId, examId);
        assertThat(hidden.visibilityState()).isEqualTo(ResultVisibilityStateDTO.PENDING_REVIEW);
        assertThat(hidden.subjectId()).isEqualTo(subjectId);
        assertThat(hidden.score()).isNull();

        resultReviewService.publish(teacherId, examId, java.util.List.of(result.getId()));

        var visible = resultReviewService.studentResult(studentId, examId);
        assertThat(visible.visibilityState()).isEqualTo(ResultVisibilityStateDTO.READY);
        assertThat(visible.subjectId()).isEqualTo(subjectId);
        assertThat(visible.score()).isEqualByComparingTo(new BigDecimal("8.0000"));
    }

    @Test
    void locksAfterClosedResultUntilEndAt() throws Exception {
        saveResult("AFTER_CLOSED", ResultReviewStatus.RELEASED, OffsetDateTime.now().plusHours(2));

        var detail = resultReviewService.studentResult(studentId, examId);

        assertThat(detail.visibilityState()).isEqualTo(ResultVisibilityStateDTO.LOCKED_UNTIL_CLOSED);
        assertThat(detail.score()).isNull();
        assertThat(detail.availableAt()).isAfter(OffsetDateTime.now());
    }

    @Test
    void adjustmentChangesTeacherEffectiveScore() throws Exception {
        ExamResult result = saveResult("AFTER_SUBMIT", ResultReviewStatus.RELEASED, OffsetDateTime.now().minusHours(1));

        var row = resultReviewService.adjustScore(teacherId, examId, result.getId(), new BigDecimal("9.25"), "Cham phuc khao");

        assertThat(row.originalScore()).isEqualByComparingTo(new BigDecimal("8.0000"));
        assertThat(row.adjustedScore()).isEqualByComparingTo(new BigDecimal("9.2500"));
        assertThat(row.effectiveScore()).isEqualByComparingTo(new BigDecimal("9.2500"));
    }

    private ExamResult saveResult(String policy, ResultReviewStatus reviewStatus, OffsetDateTime endAt) throws Exception {
        ExamResult result = new ExamResult();
        result.setExamId(examId);
        result.setStudentId(studentId);
        result.setSessionId(UUID.randomUUID());
        result.setSubmissionId(UUID.randomUUID());
        result.setAttemptNo(1);
        result.setStatus(ExamStatus.GRADED);
        result.setTotalQuestions(10);
        result.setAnsweredQuestions(10);
        result.setCorrectCount(8);
        result.setWrongCount(2);
        result.setBlankCount(0);
        result.setTotalScore(new BigDecimal("8.0000"));
        result.setMaxScore(new BigDecimal("10.0000"));
        result.setPercentage(new BigDecimal("80.000"));
        result.setSubmittedAt(OffsetDateTime.now().minusHours(2));
        result.setGradedAt(OffsetDateTime.now().minusHours(1));
        result.setGradingDurationMs(100);
        result.setGraderVersion("test");
        result.setReviewStatus(reviewStatus);
        result.setReleasedAt(reviewStatus == ResultReviewStatus.RELEASED ? OffsetDateTime.now().minusMinutes(30) : null);
        result.setExamSnapshot(objectMapper.writeValueAsString(Map.of(
                "examId", examId,
                "snapshotVersion", 1,
                "title", "Midterm",
                "subjectId", subjectId,
                "subjectName", "Math",
                "ownerTeacherId", teacherId,
                "endAt", endAt,
                "showResultPolicy", policy
        )));
        result.setStudentSnapshot(objectMapper.writeValueAsString(Map.of(
                "studentId", studentId,
                "studentCode", "S001",
                "studentName", "Nguyen Van A"
        )));
        return examResultRepo.saveAndFlush(result);
    }
}
