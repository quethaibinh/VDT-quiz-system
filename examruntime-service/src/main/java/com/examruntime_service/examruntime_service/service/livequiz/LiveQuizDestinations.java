package com.examruntime_service.examruntime_service.service.livequiz;

import java.util.UUID;

public final class LiveQuizDestinations {

    /**
     * Utility class chi chua ham static, khong cho tao instance.
     */
    private LiveQuizDestinations() {
    }

    /**
     * Topic lobby cho su kien student join va room started.
     */
    public static String lobbyTopic(UUID roomId) {
        return "/topic/live-quizzes/%s/lobby".formatted(roomId);
    }

    /**
     * Topic progress rieng cho teacher theo doi tung su kien lam bai.
     */
    public static String teacherProgressTopic(UUID roomId) {
        return "/topic/live-quizzes/%s/teacher-progress".formatted(roomId);
    }

    /**
     * Topic bang xep hang realtime cua room.
     */
    public static String leaderboardTopic(UUID roomId) {
        return "/topic/live-quizzes/%s/leaderboard".formatted(roomId);
    }

    /**
     * User queue cho message gui rieng toi mot student trong room.
     */
    public static String studentQueue(UUID roomId) {
        return "/queue/live-quizzes/%s".formatted(roomId);
    }

    /**
     * Redis channel dung lam backplane cho realtime live quiz.
     */
    public static String eventsChannel(UUID roomId) {
        return "livequiz:room:events:%s".formatted(roomId);
    }
}
