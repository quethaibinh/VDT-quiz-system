package com.result_service.result_service.service.results;

import com.result_service.result_service.model.entity.ExamResult;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Bo doc snapshot de phan giai thong tin ky thi va hoc sinh tu chuoi JSON luu trong DB.
 * Snapshot duoc dong bang tai thoi diem nop bai thi de dam bao du lieu lich su khong bi anh huong neu thong tin goc thay doi.
 */
@Component
public class ResultSnapshotReader {

    private final ObjectMapper objectMapper;

    public ResultSnapshotReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * Doc va map thong tin ky thi tu `examSnapshot` cua ket qua thi.
     * Truong hop snapshot bi thieu cac truong, su dung cac thong tin co san trong table de fallback.
     */
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

    /**
     * Doc va map thong tin hoc sinh tu `studentSnapshot` cua ket qua thi.
     * Su dung thu tu uu tien de lay code va name cua hoc sinh tu nhieu ten truong json khac nhau de dam bao do tuong thich.
     */
    public StudentSnapshotInfo student(ExamResult result) {
        JsonNode node = read(result.getStudentSnapshot());
        UUID studentId = uuid(node, "studentId", result.getStudentId());
        String code = firstText(node, "studentCode", "code");
        String name = firstText(node, "fullName", "displayName", "studentName", "name");
        return new StudentSnapshotInfo(studentId, code, name != null ? name : studentId.toString());
    }

    /**
     * Chuyen doi chuoi JSON sang doi tuong Generic.
     */
    public Object object(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (Exception exception) {
            return null;
        }
    }

    /**
     * Parse chuoi JSON sang JsonNode, neu rong hoac loi thi tra ve mot ObjectNode rong de tranh NullPointerException.
     */
    private JsonNode read(String json) {
        try {
            return objectMapper.readTree(json == null || json.isBlank() ? "{}" : json);
        } catch (Exception exception) {
            return objectMapper.createObjectNode();
        }
    }

    /**
     * Lay gia tri text dau tien khong null tu mot danh sach cac ten field kha thi.
     */
    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            String value = text(node, field);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Lay gia tri chuoi tu JsonNode, neu khong ton tai hoac null thi tra ve null.
     */
    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text;
    }

    /**
     * Lay gia tri UUID tu JsonNode, neu loi hoac null thi tra ve gia tri fallback.
     */
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

    /**
     * Lay gia tri thoi gian OffsetDateTime tu JsonNode.
     */
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

    /**
     * Lay gia tri so nguyen tu JsonNode, neu khong phai so thi tra ve gia tri fallback.
     */
    private int integer(JsonNode node, String field, int fallback) {
        JsonNode value = node.path(field);
        return value.isNumber() ? value.asInt() : fallback;
    }
}
