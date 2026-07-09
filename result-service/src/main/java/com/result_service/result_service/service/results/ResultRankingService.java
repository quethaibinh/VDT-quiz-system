package com.result_service.result_service.service.results;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Service tinh toan bang xep hang hoc sinh cho ca thi.
 * Su dung Dense/Competition Ranking: Cac hoc sinh bang diem thi dong hang; hang tiep theo bo qua vi tri (vi du: 1, 2, 2, 4).
 */
@Component
public class ResultRankingService {

    /**
     * Tinh toan thu hang cho tat ca cac ket qua trong mot ca thi.
     * Sap xep theo diem so thuc te (effective score) giam dan.
     * Neu bang diem nhau, sap xep theo thoi gian nop bai som hon (submittedAt) va cuoi cung la so sanh UUID cua Student de dam bao thu tu sap xep luon dong nhat (deterministic).
     *
     * @param results danh sach cac ExamResult cua ca thi
     * @return Map tu resultId sang thu hang (1-indexed) cua hoc sinh do
     */
    public Map<UUID, Integer> ranks(List<ExamResult> results) {
        // Sap xep danh sach ket qua dua tren cac tieu chi
        List<ExamResult> sorted = results.stream()
                .sorted(Comparator
                        .comparing(this::effectiveScore, Comparator.reverseOrder())
                        .thenComparing(ExamResult::getSubmittedAt)
                        .thenComparing(result -> result.getStudentId().toString()))
                .toList();

        Map<UUID, Integer> ranks = new HashMap<>();
        BigDecimal previousScore = null;
        int previousRank = 0;
        
        // Duyet qua danh sach da sap xep de gan thu hang
        for (int i = 0; i < sorted.size(); i++) {
            ExamResult result = sorted.get(i);
            BigDecimal score = effectiveScore(result);
            
            // Neu diem cua ban ghi hien tai bang diem voi ban ghi ngay truoc do, giu nguyen rank
            // Nguoc lai, rank bang vi tri index + 1 (vi du: 1, 2, 2, 4)
            int rank = previousScore != null && previousScore.compareTo(score) == 0
                    ? previousRank
                    : i + 1;
            ranks.put(result.getId(), rank);
            previousScore = score;
            previousRank = rank;
        }
        return ranks;
    }

    /**
     * Tinh diem thuc te cua mot ket qua thi.
     * Neu giao vien da sua diem (adjustedScore != null) thi lay diem da sua.
     * Neu khong, mac dinh lay diem goc duoc cham tu dong (totalScore).
     */
    public BigDecimal effectiveScore(ExamResult result) {
        return result.getAdjustedScore() != null ? result.getAdjustedScore() : result.getTotalScore();
    }
}
