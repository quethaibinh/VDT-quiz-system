package com.examruntime_service.examruntime_service.service.livequiz;

import com.examruntime_service.examruntime_service.model.entity.enums.LiveQuizAnswerStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class LiveQuizScoringPolicyTest {

    private final LiveQuizScoringPolicy policy = new LiveQuizScoringPolicy();
    private final OffsetDateTime startedAt = OffsetDateTime.parse("2026-07-03T10:00:00+07:00");
    private final OffsetDateTime endsAt = startedAt.plusSeconds(10);

    @Test
    void correctAnswerAtStartReceivesFullScore() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                startedAt,
                true,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("10.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("1.0000");
        assertThat(result.responseTimeMs()).isZero();
    }

    @Test
    void correctAnswerHalfwayReceivesSeventyFivePercent() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                startedAt.plusSeconds(5),
                true,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("7.5000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("0.7500");
        assertThat(result.responseTimeMs()).isEqualTo(5000);
    }

    @Test
    void correctAnswerAtDeadlineReceivesMinimumHalfScore() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                endsAt,
                true,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("5.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("0.5000");
    }

    @Test
    void wrongAnswerReceivesZeroWithResponseTime() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                startedAt.plusSeconds(3),
                false,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("0.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("0.0000");
        assertThat(result.responseTimeMs()).isEqualTo(3000);
    }

    @Test
    void timeoutReceivesZero() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                endsAt.plusSeconds(1),
                true,
                LiveQuizAnswerStatus.TIMEOUT
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("0.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("0.0000");
    }

    @Test
    void receivedBeforeStartIsClampedToFullScore() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                endsAt,
                startedAt.minusSeconds(2),
                true,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("10.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("1.0000");
        assertThat(result.responseTimeMs()).isZero();
    }

    @Test
    void invalidTimeWindowReceivesZero() {
        LiveQuizScoreResult result = policy.score(
                BigDecimal.TEN,
                startedAt,
                startedAt,
                startedAt,
                true,
                LiveQuizAnswerStatus.ANSWERED
        );

        assertThat(result.scoreAwarded()).isEqualByComparingTo("0.0000");
        assertThat(result.scoreRatio()).isEqualByComparingTo("0.0000");
    }
}
