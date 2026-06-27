package com.result_service.result_service.service.results;

import com.result_service.result_service.model.dto.results.ResultVisibilityStateDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

@Component
public class ResultPolicyResolver {

    public static final String POLICY_AFTER_SUBMIT = "AFTER_SUBMIT";
    public static final String POLICY_AFTER_CLOSED = "AFTER_CLOSED";
    public static final String POLICY_NEVER = "NEVER";

    private final ResultSnapshotReader snapshotReader;
    private final Clock clock;

    public ResultPolicyResolver(ResultSnapshotReader snapshotReader, Clock clock) {
        this.snapshotReader = snapshotReader;
        this.clock = clock;
    }

    public ResultReviewStatus initialReviewStatus(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        return POLICY_NEVER.equals(exam.showResultPolicy())
                ? ResultReviewStatus.PENDING_REVIEW
                : ResultReviewStatus.RELEASED;
    }

    public ResultVisibilityStateDTO visibility(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        ResultReviewStatus reviewStatus = result.getReviewStatus() != null
                ? result.getReviewStatus()
                : ResultReviewStatus.PENDING_REVIEW;
        if (reviewStatus == ResultReviewStatus.PENDING_REVIEW) {
            return ResultVisibilityStateDTO.PENDING_REVIEW;
        }
        if (exam.showResultPolicy() == null) {
            return ResultVisibilityStateDTO.CONFIG_MISSING;
        }
        if (POLICY_AFTER_CLOSED.equals(exam.showResultPolicy())
                && exam.endAt() != null
                && OffsetDateTime.now(clock).isBefore(exam.endAt())) {
            return ResultVisibilityStateDTO.LOCKED_UNTIL_CLOSED;
        }
        return ResultVisibilityStateDTO.READY;
    }

    public boolean visibleToStudent(ExamResult result) {
        ResultVisibilityStateDTO state = visibility(result);
        return state == ResultVisibilityStateDTO.READY || state == ResultVisibilityStateDTO.RELEASED;
    }

    public OffsetDateTime availableAt(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        if (POLICY_AFTER_CLOSED.equals(exam.showResultPolicy())) {
            return exam.endAt();
        }
        return result.getReleasedAt();
    }

    public String message(ResultVisibilityStateDTO state) {
        return switch (state) {
            case READY, RELEASED -> "RESULT_AVAILABLE";
            case PENDING_REVIEW -> "WAITING_FOR_TEACHER_REVIEW";
            case LOCKED_UNTIL_CLOSED -> "RESULT_AVAILABLE_AFTER_EXAM_CLOSED";
            case CONFIG_MISSING -> "RESULT_VISIBILITY_CONFIG_MISSING";
            case GRADING -> "RESULT_GRADING";
            case GRADING_FAILED -> "RESULT_GRADING_FAILED";
        };
    }
}
