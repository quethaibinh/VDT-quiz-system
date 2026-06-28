package com.result_service.result_service.service.results;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ResultRankingService {

    public Map<UUID, Integer> ranks(List<ExamResult> results) {
        List<ExamResult> sorted = results.stream()
                .sorted(Comparator
                        .comparing(this::effectiveScore, Comparator.reverseOrder())
                        .thenComparing(ExamResult::getSubmittedAt)
                        .thenComparing(result -> result.getStudentId().toString()))
                .toList();

        Map<UUID, Integer> ranks = new HashMap<>();
        BigDecimal previousScore = null;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            ExamResult result = sorted.get(i);
            BigDecimal score = effectiveScore(result);
            int rank = previousScore != null && previousScore.compareTo(score) == 0
                    ? previousRank
                    : i + 1;
            ranks.put(result.getId(), rank);
            previousScore = score;
            previousRank = rank;
        }
        return ranks;
    }

    public BigDecimal effectiveScore(ExamResult result) {
        return result.getAdjustedScore() != null ? result.getAdjustedScore() : result.getTotalScore();
    }
}
