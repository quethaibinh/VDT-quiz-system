package com.question_service.question_service.service.media;

import com.question_service.question_service.config.minio.MinioProperties;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class QuestionMediaStorageService {

    private static final String TEMP_PREFIX = "questions/temp/images/";

    private final MinioClient minioClient;
    private final MinioClient publicMinioClient;
    private final MinioProperties properties;
    private final Set<String> allowedTypes;

    public QuestionMediaStorageService(
            @Qualifier("questionMediaMinioClient") MinioClient minioClient,
            @Qualifier("questionMediaPublicMinioClient") MinioClient publicMinioClient,
            MinioProperties properties
    ) {
        this.minioClient = minioClient;
        this.publicMinioClient = publicMinioClient;
        this.properties = properties;
        this.allowedTypes = properties.allowedTypes().stream()
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_MEDIA_FILE_REQUIRED");
        }
        if (file.getSize() > properties.maxBytes()) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_MEDIA_TOO_LARGE");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!allowedTypes.contains(contentType)) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_MEDIA_TYPE_NOT_ALLOWED");
        }
        String objectKey = TEMP_PREFIX + UUID.randomUUID() + extension(contentType);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(properties.bucket())
                    .object(objectKey)
                    .contentType(contentType)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .build());
            return objectKey;
        } catch (Exception exception) {
            throw new IllegalStateException("QUESTION_MEDIA_UPLOAD_FAILED", exception);
        }
    }

    public String signedReadUrl(String objectKey) {
        String safeKey = validateObjectKey(objectKey);
        try {
            return publicMinioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(properties.bucket())
                    .object(safeKey)
                    .expiry(properties.signedUrlTtlSeconds(), TimeUnit.SECONDS)
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("QUESTION_MEDIA_SIGN_URL_FAILED", exception);
        }
    }

    public String validateObjectKey(String objectKey) {
        String value = objectKey == null ? null : objectKey.trim();
        if (value == null || value.isBlank()) {
            return null;
        }
        if (!value.startsWith("questions/")
                || value.contains("..")
                || value.startsWith("/")
                || value.contains("\\")
                || value.contains("//")) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_MEDIA_OBJECT_KEY_INVALID");
        }
        return value;
    }

    public int signedUrlTtlSeconds() {
        return properties.signedUrlTtlSeconds();
    }

    private String normalizeContentType(String contentType) {
        return contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
    }

    private String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/jpeg" -> ".jpg";
            case "image/webp" -> ".webp";
            default -> throw error(HttpStatus.BAD_REQUEST, "QUESTION_MEDIA_TYPE_NOT_ALLOWED");
        };
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
