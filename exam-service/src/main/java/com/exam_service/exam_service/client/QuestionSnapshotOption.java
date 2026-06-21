package com.exam_service.exam_service.client;

import java.util.UUID;

public record QuestionSnapshotOption(
        UUID optionId,
        String key,
        String content,
        String contentFormat,
        boolean correct
) {
}
