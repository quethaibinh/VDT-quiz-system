package com.result_service.result_service.service.results;

import com.result_service.result_service.model.dto.results.ResultVisibilityStateDTO;
import com.result_service.result_service.model.entity.ExamResult;
import com.result_service.result_service.model.entity.enums.ResultReviewStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.OffsetDateTime;

/**
 * Bo phan giai quyet chinh sach hien thi diem thi cho hoc sinh.
 * Dua tren `showResultPolicy` cua ky thi trong snapshot de quyet dinh hoc sinh co duoc xem diem hay chua.
 */
@Component
public class ResultPolicyResolver {

    // Cac loai chinh sach hien thi diem:
    // 1. AFTER_SUBMIT: Hoc sinh duoc xem ngay sau khi nop va cham diem xong.
    // 2. AFTER_CLOSED: Hoc sinh chi duoc xem sau khi ca thi ket thuc (dua tren endAt).
    // 3. NEVER: Hoc sinh chi xem duoc sau khi giao vien chu dong publish ket qua (con goi la MANUAL_RELEASE_REQUIRED).
    public static final String POLICY_AFTER_SUBMIT = "AFTER_SUBMIT";
    public static final String POLICY_AFTER_CLOSED = "AFTER_CLOSED";
    public static final String POLICY_NEVER = "NEVER";

    private final ResultSnapshotReader snapshotReader;
    private final Clock clock;

    public ResultPolicyResolver(ResultSnapshotReader snapshotReader, Clock clock) {
        this.snapshotReader = snapshotReader;
        this.clock = clock;
    }

    /**
     * Xac dinh trang thai duyet ban dau khi moi cham xong.
     * Neu policy la NEVER thi trang thai review mac dinh la PENDING_REVIEW.
     * Cac policy khac mac dinh la RELEASED (cho phep phan giai o buoc sau).
     */
    public ResultReviewStatus initialReviewStatus(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        return POLICY_NEVER.equals(exam.showResultPolicy())
                ? ResultReviewStatus.PENDING_REVIEW
                : ResultReviewStatus.RELEASED;
    }

    /**
     * Phan tich trang thai hien thi thuc te (Visibility State) cua ket qua thi.
     * 
     * @return ResultVisibilityStateDTO cac trang thai nhu: READY, PENDING_REVIEW, LOCKED_UNTIL_CLOSED,...
     */
    public ResultVisibilityStateDTO visibility(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        ResultReviewStatus reviewStatus = result.getReviewStatus() != null
                ? result.getReviewStatus()
                : ResultReviewStatus.PENDING_REVIEW;
                
        // Neu giao vien chua phe duyet (voi policy NEVER), ket qua o trang thai PENDING_REVIEW
        if (reviewStatus == ResultReviewStatus.PENDING_REVIEW) {
            return ResultVisibilityStateDTO.PENDING_REVIEW;
        }
        
        // Neu khong co thong tin ve policy hien thi
        if (exam.showResultPolicy() == null) {
            return ResultVisibilityStateDTO.CONFIG_MISSING;
        }
        
        // Neu policy la AFTER_CLOSED nhung hien tai chua toi thoi diem ket thuc ca thi (endAt)
        if (POLICY_AFTER_CLOSED.equals(exam.showResultPolicy())
                && exam.endAt() != null
                && OffsetDateTime.now(clock).isBefore(exam.endAt())) {
            return ResultVisibilityStateDTO.LOCKED_UNTIL_CLOSED;
        }
        
        // San sang hien thi diem cho hoc sinh
        return ResultVisibilityStateDTO.READY;
    }

    /**
     * Kiem tra xem hoc sinh co the xem diem cu the cua minh hay chua.
     */
    public boolean visibleToStudent(ExamResult result) {
        ResultVisibilityStateDTO state = visibility(result);
        return state == ResultVisibilityStateDTO.READY || state == ResultVisibilityStateDTO.RELEASED;
    }

    /**
     * Tra ve thoi diem ma hoc sinh co the bat dau xem diem.
     * Neu policy la AFTER_CLOSED, thi do la thoi diem endAt cua ca thi.
     * Neu khong, do la thoi diem ma ket qua duoc released thuc te.
     */
    public OffsetDateTime availableAt(ExamResult result) {
        ExamSnapshotInfo exam = snapshotReader.exam(result);
        if (POLICY_AFTER_CLOSED.equals(exam.showResultPolicy())) {
            return exam.endAt();
        }
        return result.getReleasedAt();
    }

    /**
     * Map trang thai hien thi thuc te sang ma thong bao nghiep vu phu hop.
     */
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
