package com.question_service.question_service.config.minio;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "question.media")
public record MinioProperties(
        String endpoint,
        String publicEndpoint,
        String accessKey,
        String secretKey,
        String bucket,
        long maxBytes,
        List<String> allowedTypes,
        int signedUrlTtlSeconds
) {
    public MinioProperties {
        endpoint = endpoint == null || endpoint.isBlank() ? "http://localhost:9000" : endpoint;
        publicEndpoint = publicEndpoint == null || publicEndpoint.isBlank() ? endpoint : publicEndpoint;
        accessKey = accessKey == null || accessKey.isBlank() ? "minioadmin" : accessKey;
        secretKey = secretKey == null || secretKey.isBlank() ? "minioadmin" : secretKey;
        bucket = bucket == null || bucket.isBlank() ? "quiz-question-media" : bucket;
        maxBytes = maxBytes <= 0 ? 5_242_880L : maxBytes;
        allowedTypes = allowedTypes == null || allowedTypes.isEmpty()
                ? List.of("image/png", "image/jpeg", "image/webp")
                : allowedTypes;
        signedUrlTtlSeconds = signedUrlTtlSeconds <= 0 ? 900 : signedUrlTtlSeconds;
    }
}
