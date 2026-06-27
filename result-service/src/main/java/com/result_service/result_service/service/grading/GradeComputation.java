package com.result_service.result_service.service.grading;

import com.result_service.result_service.model.entity.ResultAnswer;

import java.math.BigDecimal;
import java.util.List;

record GradeComputation(
        List<ResultAnswer> answers,
        int totalQuestions,
        int answeredQuestions,
        int correctCount,
        int wrongCount,
        int blankCount,
        BigDecimal totalScore,
        BigDecimal maxScore,
        BigDecimal percentage
) {
}
