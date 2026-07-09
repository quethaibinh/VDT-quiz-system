package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

@Component
public class LiveQuizScoringPolicy {

    private static final int SCORE_SCALE = 4;
    private static final int RATIO_SCALE = 4;
    private static final BigDecimal HALF = BigDecimal.valueOf(0.5);
    private static final BigDecimal ZERO_RATIO = BigDecimal.ZERO.setScale(RATIO_SCALE, RoundingMode.HALF_UP);

    public LiveQuizScoreResult score(
            BigDecimal baseScore,
            OffsetDateTime startedAt,
            OffsetDateTime endsAt,
            OffsetDateTime receivedAt,
            boolean correct,
            LiveQuizAnswerStatus status
    ) {
        int responseTimeMs = safeResponseTimeMs(startedAt, receivedAt);
        if (!correct || status == LiveQuizAnswerStatus.TIMEOUT) {
            return new LiveQuizScoreResult(BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP), responseTimeMs, ZERO_RATIO);
        }
        long timeLimitMs = millisBetween(startedAt, endsAt);
        if (baseScore == null || baseScore.compareTo(BigDecimal.ZERO) <= 0 || timeLimitMs <= 0) {
            return new LiveQuizScoreResult(BigDecimal.ZERO.setScale(SCORE_SCALE, RoundingMode.HALF_UP), responseTimeMs, ZERO_RATIO);
        }
        long remainingMs = clamp(millisBetween(receivedAt, endsAt), 0, timeLimitMs);
        BigDecimal remainingRatio = BigDecimal.valueOf(remainingMs)
                .divide(BigDecimal.valueOf(timeLimitMs), 8, RoundingMode.HALF_UP);
        BigDecimal scoreRatio = HALF.add(HALF.multiply(remainingRatio)).setScale(RATIO_SCALE, RoundingMode.HALF_UP);
        BigDecimal score = baseScore.multiply(scoreRatio).setScale(SCORE_SCALE, RoundingMode.HALF_UP);
        return new LiveQuizScoreResult(score, responseTimeMs, scoreRatio);
    }

    private int safeResponseTimeMs(OffsetDateTime startedAt, OffsetDateTime receivedAt) {
        long responseTimeMs = Math.max(0, millisBetween(startedAt, receivedAt));
        if (responseTimeMs > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) responseTimeMs;
    }

    private long millisBetween(OffsetDateTime start, OffsetDateTime end) {
        if (start == null || end == null) {
            return 0;
        }
        return ChronoUnit.MILLIS.between(start, end);
    }

    private long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(max, value));
    }
}
