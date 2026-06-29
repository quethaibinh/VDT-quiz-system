package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.dto.events.SubmissionCreatedEvent;
import com.result_service.result_service.model.entity.ResultAnswer;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Bo tinh diem thuc hien cham bai thi trac nghiem dua tren thong tin tu SubmissionCreatedEvent.
 * Diem so duoc tinh theo thang diem 10 mac dinh (NORMALIZED_MAX_SCORE) chia deu cho tong so cau hoi.
 */
@Component
public class GradeCalculator {

    private static final BigDecimal NORMALIZED_MAX_SCORE = BigDecimal.TEN;
    private static final int INTERNAL_SCORE_SCALE = 10;

    private final GradingJsonSupport jsonSupport;

    public GradeCalculator(GradingJsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    /**
     * Tinh toan ket qua thi va sinh danh sach chi tiet cau tra loi.
     * Quy trinh gom: Validate dau vao -> Nhom cau tra loi cua hoc sinh -> Sap xep cau hoi ->
     * Chia deu diem cho cac cau hoi -> Cham tung cau hoi -> Tinh toan ty le phan tram dung.
     *
     * @param event thong tin submission tu he thong runtime
     * @return GradeComputation chua thong ke so cau dung/sai/trong va chi tiet diem so
     */
    public GradeComputation compute(SubmissionCreatedEvent event) {
        // Validate tinh hop le cua snapshot ky thi va cau tra loi cua hoc sinh
        validateGradingSnapshot(event);

        // Nhom cau tra loi cua hoc sinh theo questionId.
        // Neu hoc sinh nop nhieu hon mot cau tra loi cho cung mot cau hoi, he thong lay ban ghi cuoi cung (right).
        Map<UUID, List<UUID>> selectedByQuestion = event.answerSnapshot().stream()
                .collect(Collectors.toMap(
                        SubmissionCreatedEvent.AnswerSnapshotItem::questionId,
                        answer -> normalizeIds(answer.selectedOptionIds()),
                        (left, right) -> right
                ));

        // Lay danh sach cau hoi tu snapshot va sap xep theo dung thu tu (questionOrder) duoc thiet lap.
        List<SubmissionCreatedEvent.QuestionSnapshot> questions = new ArrayList<>(event.paperSnapshot().questions());
        questions.sort(Comparator.comparingInt(question -> question.questionOrder() != null
                ? question.questionOrder()
                : Integer.MAX_VALUE));

        List<ResultAnswer> answers = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        int answered = 0;
        int correct = 0;
        int index = 0;
        int totalQuestions = questions.size();
        
        // Diem toi da cho moi cau = 10.0 / tong so cau hoi.
        // Dung scale = 10 de tranh lam tron qua som lam sai lech tong diem cuoi cung.
        BigDecimal questionMax = NORMALIZED_MAX_SCORE.divide(
                BigDecimal.valueOf(totalQuestions),
                INTERNAL_SCORE_SCALE,
                RoundingMode.HALF_UP
        );

        // Duyet qua tung cau hoi trong bai thi de cham diem
        for (SubmissionCreatedEvent.QuestionSnapshot question : questions) {
            index++;
            // Chuan hoa danh sach id cac lua chon cua hoc sinh va dap an dung
            List<UUID> selected = normalizeIds(selectedByQuestion.get(question.questionId()));
            List<UUID> correctOptions = normalizeIds(question.correctOptionIds());
            
            // Xac dinh hoc sinh co bo trong cau nay khong
            boolean blank = selected.isEmpty();
            
            // So sanh 2 tap hop de xac dinh dung/sai. Neu khong blank va cac lua chon trung khop thi dung.
            boolean isCorrect = !blank && sameIds(selected, correctOptions);
            BigDecimal awarded = isCorrect ? questionMax : BigDecimal.ZERO;

            if (!blank) {
                answered++;
            }
            if (isCorrect) {
                correct++;
            }
            totalScore = totalScore.add(awarded);

            // Tao doi tuong ResultAnswer de luu vao DB
            answers.add(buildAnswer(event, question, index, selected, correctOptions, blank, isCorrect, awarded, questionMax));
        }

        // Tinh toan so cau bo trong va so cau lam sai
        int blank = totalQuestions - answered;
        int wrong = answered - correct;
        
        // Tinh phan tram hoan thanh chinh xac: (totalScore * 100) / 10.0, lam tron den 3 chu so thap phan
        BigDecimal percentage = totalScore.multiply(BigDecimal.valueOf(100))
                .divide(NORMALIZED_MAX_SCORE, 3, RoundingMode.HALF_UP);

        return new GradeComputation(
                answers,
                totalQuestions,
                answered,
                correct,
                wrong,
                blank,
                totalScore.setScale(4, RoundingMode.HALF_UP),
                NORMALIZED_MAX_SCORE.setScale(4, RoundingMode.HALF_UP),
                percentage
        );
    }

    /**
     * Khoi tao va dinh dang thong tin thuc the ResultAnswer de luu tru.
     */
    private ResultAnswer buildAnswer(
            SubmissionCreatedEvent event,
            SubmissionCreatedEvent.QuestionSnapshot question,
            int fallbackOrder,
            List<UUID> selected,
            List<UUID> correctOptions,
            boolean blank,
            boolean isCorrect,
            BigDecimal awarded,
            BigDecimal questionMax
    ) {
        ResultAnswer answer = new ResultAnswer();
        answer.setExamId(event.examId());
        answer.setQuestionId(question.questionId());
        answer.setQuestionOrder(question.questionOrder() != null ? question.questionOrder() : fallbackOrder);
        answer.setSelectedOptionIds(jsonSupport.toJson(selected));
        answer.setCorrectOptionIds(jsonSupport.toJson(correctOptions));
        answer.setCorrect(isCorrect);
        // Diem dat duoc va diem toi da cua cau hoi duoc lam tron ve 3 chu so thap phan de luu tru dep hon
        answer.setScoreAwarded(awarded.setScale(3, RoundingMode.HALF_UP));
        answer.setMaxScore(questionMax.setScale(3, RoundingMode.HALF_UP));
        answer.setGradingNote(blank ? "BLANK" : isCorrect ? "CORRECT" : "WRONG");
        answer.setQuestionSnapshot(jsonSupport.toJson(question.questionSnapshot() != null
                ? question.questionSnapshot()
                : question));
        return answer;
    }

    /**
     * Kiem tra tinh toan ven cua du lieu snapshot truoc khi cham diem.
     * Tat ca cac thieu sot nghiep vu nhu thieu danh sach cau hoi, thieu dap an dung deu se nem ra loi.
     */
    private void validateGradingSnapshot(SubmissionCreatedEvent event) {
        if (event.answerSnapshot() == null) {
            throw new IllegalArgumentException("ANSWER_SNAPSHOT_REQUIRED");
        }
        if (event.paperSnapshot() == null
                || event.paperSnapshot().questions() == null
                || event.paperSnapshot().questions().isEmpty()) {
            throw new IllegalArgumentException("PAPER_SNAPSHOT_QUESTIONS_REQUIRED");
        }
        Set<UUID> questionIds = event.paperSnapshot().questions().stream()
                .map(SubmissionCreatedEvent.QuestionSnapshot::questionId)
                .collect(Collectors.toSet());
        if (questionIds.size() != event.paperSnapshot().questions().size() || questionIds.contains(null)) {
            throw new IllegalArgumentException("PAPER_SNAPSHOT_QUESTION_IDS_INVALID");
        }
        event.paperSnapshot().questions().forEach(question -> {
            if (question.correctOptionIds() == null || question.correctOptionIds().isEmpty()) {
                throw new IllegalArgumentException("QUESTION_CORRECT_OPTIONS_REQUIRED");
            }
        });
        event.answerSnapshot().forEach(answer -> {
            if (answer.questionId() == null) {
                throw new IllegalArgumentException("ANSWER_QUESTION_ID_REQUIRED");
            }
            if (!questionIds.contains(answer.questionId())) {
                throw new IllegalArgumentException("ANSWER_QUESTION_NOT_IN_PAPER");
            }
        });
    }

    /**
     * So sanh danh sach cac lua chon hoc sinh chon co khop hoan toan voi dap an dung hay khong.
     * Su dung LinkedHashSet de khong quan tam den thu tu lua chon ma chi so sanh cac phan tu.
     */
    private boolean sameIds(List<UUID> selected, List<UUID> correct) {
        return new LinkedHashSet<>(selected).equals(new LinkedHashSet<>(correct));
    }

    /**
     * Chuan hoa list UUID: loai bo null, loai bo cac phan tu trung lap (distinct), sap xep tang dan.
     * Muc dich la de phep so sanh equals cua Set/List giua selected va correct luon chinh xac va dong nhat.
     */
    private List<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null).distinct().sorted().toList();
    }
}
