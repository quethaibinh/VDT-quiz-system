package com.examruntime_service.examruntime_service.service.monitor;

import java.util.UUID;

public final class MonitorDestinations {

    private MonitorDestinations() {
    }

    public static String teacherTopic(UUID examId) {
        return "/topic/exams/%s/monitor".formatted(examId);
    }

    public static String studentAlertQueue(UUID examId) {
        return "/queue/exams/%s/alerts".formatted(examId);
    }
}
