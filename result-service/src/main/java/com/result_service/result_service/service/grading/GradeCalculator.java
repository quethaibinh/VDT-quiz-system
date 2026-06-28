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

@Component
public class GradeCalculator {

    private final GradingJsonSupport jsonSupport;

    public GradeCalculator(GradingJsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public GradeComputation compute(SubmissionCreatedEvent event) {
        validateGradingSnapshot(event);

        // Gom cau tra loi theo questionId; neu duplicate thi lay ban ghi cuoi cung.
        Map<UUID, List<UUID>> selectedByQuestion = event.answerSnapshot().stream()
                .collect(Collectors.toMap(
                        SubmissionCreatedEvent.AnswerSnapshotItem::questionId,
                        answer -> normalizeIds(answer.selectedOptionIds()),
                        (left, right) -> right
                ));

        List<SubmissionCreatedEvent.QuestionSnapshot> questions = new ArrayList<>(event.paperSnapshot().questions());
        questions.sort(Comparator.comparingInt(question -> question.questionOrder() != null
                ? question.questionOrder()
                : Integer.MAX_VALUE));

        List<ResultAnswer> answers = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        int answered = 0;
        int correct = 0;
        int index = 0;

        for (SubmissionCreatedEvent.QuestionSnapshot question : questions) {
            index++;
            List<UUID> selected = normalizeIds(selectedByQuestion.get(question.questionId()));
            List<UUID> correctOptions = normalizeIds(question.correctOptionIds());
            BigDecimal questionMax = question.effectiveMaxScore();
            boolean blank = selected.isEmpty();
            boolean isCorrect = !blank && sameIds(selected, correctOptions);
            BigDecimal awarded = isCorrect ? questionMax : BigDecimal.ZERO;

            if (!blank) {
                answered++;
            }
            if (isCorrect) {
                correct++;
            }
            totalScore = totalScore.add(awarded);
            maxScore = maxScore.add(questionMax);

            answers.add(buildAnswer(event, question, index, selected, correctOptions, blank, isCorrect, awarded));
        }

        int totalQuestions = questions.size();
        int blank = totalQuestions - answered;
        int wrong = answered - correct;
        BigDecimal percentage = maxScore.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : totalScore.multiply(BigDecimal.valueOf(100)).divide(maxScore, 3, RoundingMode.HALF_UP);

        return new GradeComputation(
                answers,
                totalQuestions,
                answered,
                correct,
                wrong,
                blank,
                totalScore.setScale(4, RoundingMode.HALF_UP),
                maxScore.setScale(4, RoundingMode.HALF_UP),
                percentage
        );
    }

    private ResultAnswer buildAnswer(
            SubmissionCreatedEvent event,
            SubmissionCreatedEvent.QuestionSnapshot question,
            int fallbackOrder,
            List<UUID> selected,
            List<UUID> correctOptions,
            boolean blank,
            boolean isCorrect,
            BigDecimal awarded
    ) {
        ResultAnswer answer = new ResultAnswer();
        answer.setExamId(event.examId());
        answer.setQuestionId(question.questionId());
        answer.setQuestionOrder(question.questionOrder() != null ? question.questionOrder() : fallbackOrder);
        answer.setSelectedOptionIds(jsonSupport.toJson(selected));
        answer.setCorrectOptionIds(jsonSupport.toJson(correctOptions));
        answer.setCorrect(isCorrect);
        answer.setScoreAwarded(awarded.setScale(3, RoundingMode.HALF_UP));
        answer.setMaxScore(question.effectiveMaxScore().setScale(3, RoundingMode.HALF_UP));
        answer.setGradingNote(blank ? "BLANK" : isCorrect ? "CORRECT" : "WRONG");
        answer.setQuestionSnapshot(jsonSupport.toJson(question.questionSnapshot() != null
                ? question.questionSnapshot()
                : question));
        return answer;
    }

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
            if (question.effectiveMaxScore().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("QUESTION_SCORE_INVALID");
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

    private boolean sameIds(List<UUID> selected, List<UUID> correct) {
        return new LinkedHashSet<>(selected).equals(new LinkedHashSet<>(correct));
    }

    private List<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(id -> id != null).distinct().sorted().toList();
    }
}
