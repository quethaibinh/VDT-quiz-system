package com.result_service.result_service.service.livequiz;

import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class LiveQuizResultJsonSupport {

    private final ObjectMapper objectMapper;

    public LiveQuizResultJsonSupport(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_RESULT_JSON_WRITE_FAILED", exception);
        }
    }

    public <T> T fromJson(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception exception) {
            throw new IllegalArgumentException("LIVE_QUIZ_RESULT_JSON_READ_FAILED", exception);
        }
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception exception) {
            throw new IllegalStateException("LIVE_QUIZ_RESULT_HASH_FAILED", exception);
        }
    }
}
