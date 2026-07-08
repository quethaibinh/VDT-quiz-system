package com.exam_service.exam_service.client;

import java.util.List;
import java.util.UUID;

public record QuestionSnapshotItem(
        UUID questionId,
        long questionVersion,
        String difficulty,
        String type,
        String content,
        String contentFormat,
        Double defaultScore,
        Integer estimatedSecond,
        String imageObjectKey,
        List<QuestionSnapshotOption> options
) {
    public QuestionSnapshotItem(
            UUID questionId,
            long questionVersion,
            String difficulty,
            String type,
            String content,
            String contentFormat,
            Double defaultScore,
            Integer estimatedSecond,
            List<QuestionSnapshotOption> options
    ) {
        this(
                questionId,
                questionVersion,
                difficulty,
                type,
                content,
                contentFormat,
                defaultScore,
                estimatedSecond,
                null,
                options
        );
    }

    public QuestionSnapshotItem(
            UUID questionId,
            long questionVersion,
            String difficulty,
            String type,
            String content,
            String contentFormat,
            Double defaultScore,
            List<QuestionSnapshotOption> options
    ) {
        this(
                questionId,
                questionVersion,
                difficulty,
                type,
                content,
                contentFormat,
                defaultScore,
                null,
                null,
                options
        );
    }
}
