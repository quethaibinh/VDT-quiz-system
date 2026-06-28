package com.result_service.result_service.service.results;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class ResultSnapshotReader {

    private final ObjectMapper objectMapper;

    public ResultSnapshotReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExamSnapshotInfo exam(ExamResult result) {
        JsonNode node = read(result.getExamSnapshot());
        return new ExamSnapshotInfo(
                uuid(node, "examId", result.getExamId()),
                integer(node, "snapshotVersion", 0),
                text(node, "code"),
                text(node, "title"),
                uuid(node, "subjectId", null),
                text(node, "subjectName"),
                uuid(node, "ownerTeacherId", null),
                time(node, "startAt"),
                time(node, "endAt"),
                text(node, "showResultPolicy")
        );
    }

    public StudentSnapshotInfo student(ExamResult result) {
        JsonNode node = read(result.getStudentSnapshot());
        UUID studentId = uuid(node, "studentId", result.getStudentId());
        String code = firstText(node, "studentCode", "code");
        String name = firstText(node, "fullName", "displayName", "studentName", "name");
        return new StudentSnapshotInfo(studentId, code, name != null ? name : studentId.toString());
    }

    public Object object(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    private UUID uuid(JsonNode node, String field, UUID fallback) {
        String value = text(node, field);
        if (value == null) {
            return fallback;
        }
        try {
            return UUID.fromString(value);
        } catch (Exception exception) {
            return fallback;
        }
    }

    private OffsetDateTime time(JsonNode node, String field) {
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

    private int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : fallback;
    }
}
