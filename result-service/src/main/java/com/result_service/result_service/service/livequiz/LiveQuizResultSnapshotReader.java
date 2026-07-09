package com.result_service.result_service.service.livequiz;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
class LiveQuizResultSnapshotReader {

    private final ObjectMapper objectMapper;

    LiveQuizResultSnapshotReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    LiveQuizResultSnapshot read(ExamResult result) {
        JsonNode exam = readTree(result.getExamSnapshot());
        return new LiveQuizResultSnapshot(
                text(exam, "roomCode"),
                firstNonBlank(text(exam, "quizTitle"), text(exam, "title")),
                uuid(exam, "subjectId"),
                text(exam, "subjectName"),
                uuid(exam, "ownerTeacherId"),
                date(exam, "closedAt"),
                number(exam, "finalRank"),
                number(exam, "participantCount"),
                number(exam, "timeoutCount"),
                number(exam, "notReachedCount")
        );
    }

    String studentCode(ExamResult result) {
        return text(readTree(result.getStudentSnapshot()), "studentCode");
    }

    String studentName(ExamResult result) {
        String name = text(readTree(result.getStudentSnapshot()), "studentName");
        return firstNonBlank(name, result.getStudentId() != null ? result.getStudentId().toString() : "");
    }

    JsonNode readTree(String json) {
        try {
            if (json == null || json.isBlank()) {
                return objectMapper.createObjectNode();
            }
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private int number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : 0;
    }

    private UUID uuid(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private OffsetDateTime date(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception exception) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
