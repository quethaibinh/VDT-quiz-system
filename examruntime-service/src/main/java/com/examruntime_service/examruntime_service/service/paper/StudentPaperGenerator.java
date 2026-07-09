package com.examruntime_service.examruntime_service.service.paper;

import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
// Generator sinh de thi ca nhan hoa cho hoc sinh mot cach deterministic dua tren seed
public class StudentPaperGenerator {

    // Sinh long seed tu examId, studentId va snapshotVersion bang SHA-256
    public long generateSeed(UUID examId, UUID studentId, int snapshotVersion) {
        try {
            String combined = examId.toString() + "_" + studentId.toString() + "_" + snapshotVersion;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(combined.getBytes(StandardCharsets.UTF_8));
            long seed = 0;
            for (int i = 0; i < 8; i++) {
                seed = (seed << 8) | (hash[i] & 0xff);
            }
            return seed;
        } catch (Exception e) {
            throw new IllegalStateException("SEED_GENERATION_FAILED", e);
        }
    }

    // Cau truc luu thu tu cau hoi va lua chon sau khi shuffle
    public static class PaperStructure {
        public final List<UUID> questionOrder;
        public final java.util.Map<UUID, List<UUID>> optionOrders;

        public PaperStructure(List<UUID> questionOrder, java.util.Map<UUID, List<UUID>> optionOrders) {
            this.questionOrder = questionOrder;
            this.optionOrders = optionOrders;
        }
    }

    // Sinh de thi tu pool va thiet lap thu tu cau hoi/lua chon
    public PaperStructure generatePaperStructure(ExamPaperPoolDTO pool, long seed) {
        Random random = new Random(seed);

        // 1. Phan chia cau hoi theo do kho
        List<PaperQuestionDTO> easy = new ArrayList<>();
        List<PaperQuestionDTO> medium = new ArrayList<>();
        List<PaperQuestionDTO> hard = new ArrayList<>();

        for (PaperQuestionDTO q : pool.questions()) {
            String diff = q.difficulty() != null ? q.difficulty().toUpperCase() : "EASY";
            if (diff.contains("EASY")) {
                easy.add(q);
            } else if (diff.contains("HARD")) {
                hard.add(q);
            } else {
                medium.add(q);
            }
        }

        // 2. Tron rieng tung nhom va lay dung quota cau hoi
        Collections.shuffle(easy, random);
        Collections.shuffle(medium, random);
        Collections.shuffle(hard, random);

        ensureEnoughQuestions(easy, pool.easyCount());
        ensureEnoughQuestions(medium, pool.mediumCount());
        ensureEnoughQuestions(hard, pool.hardCount());

        List<PaperQuestionDTO> selected = new ArrayList<>();
        selected.addAll(easy.subList(0, pool.easyCount()));
        selected.addAll(medium.subList(0, pool.mediumCount()));
        selected.addAll(hard.subList(0, pool.hardCount()));

        // 3. Tron chung cac cau hoi da duoc chon de ra de cuoi cung
        Collections.shuffle(selected, random);

        List<UUID> questionOrder = selected.stream()
                .map(PaperQuestionDTO::questionId)
                .collect(Collectors.toList());

        java.util.Map<UUID, List<UUID>> optionOrders = new java.util.HashMap<>();
        for (PaperQuestionDTO q : selected) {
            List<PaperOptionDTO> options = new ArrayList<>(q.options());
            Collections.shuffle(options, random);
            List<UUID> optionIds = options.stream()
                    .map(PaperOptionDTO::optionId)
                    .collect(Collectors.toList());
            optionOrders.put(q.questionId(), optionIds);
        }

        return new PaperStructure(questionOrder, optionOrders);
    }

    private void ensureEnoughQuestions(List<PaperQuestionDTO> questions, int requiredCount) {
        if (questions.size() < requiredCount) {
            throw new IllegalStateException("INSUFFICIENT_PAPER_POOL");
        }
    }
}
