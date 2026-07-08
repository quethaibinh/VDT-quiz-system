package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.dto.common.PageResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionResponseDTO;
import com.question_service.question_service.model.entity.*;
import com.question_service.question_service.model.entity.enums.*;
import com.question_service.question_service.repository.QuestionCollectionRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.repository.TopicRepo;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
/**
 * Tim kiem cau hoi theo quyen truy cap, bo loc, phan trang va sap xep.
 */
public class TeacherQuestionSearchService {

    private static final Set<String> SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "difficulty", "content", "questionType"
    );

    private final QuestionRepo questionRepo;
    private final TopicRepo topicRepo;
    private final QuestionCollectionRepo collectionRepo;
    private final TeacherSubjectAccessService subjectAccessService;
    private final ObjectMapper objectMapper;

    public TeacherQuestionSearchService(
            QuestionRepo questionRepo,
            TopicRepo topicRepo,
            QuestionCollectionRepo collectionRepo,
            TeacherSubjectAccessService subjectAccessService,
            ObjectMapper objectMapper
    ) {
        this.questionRepo = questionRepo;
        this.topicRepo = topicRepo;
        this.collectionRepo = collectionRepo;
        this.subjectAccessService = subjectAccessService;
        this.objectMapper = objectMapper;
    }

    /**
     * Tra ve trang cau hoi ma giao vien duoc phep truy cap trong mon hoc.
     */
    public PageResponseDTO<QuestionResponseDTO> search(
            UUID subjectId,
            UUID teacherId,
            String keyword,
            UUID topicId,
            String difficulty,
            String visibility,
            String ownerScope,
            String questionType,
            UUID collectionId,
            String membership,
            int page,
            int size,
            String sort
    ) {
        QuestionSearchCriteria criteria = criteria(
                subjectId, teacherId, keyword, topicId, difficulty, visibility,
                ownerScope, questionType, collectionId, membership, null
        );
        Page<QuestionResponseDTO> result = questionRepo.findAll(
                        QuestionSpecifications.accessible(subjectId, teacherId, criteria),
                        pageable(page, size, sort)
                )
                .map(this::toResponse);
        return PageResponseDTO.from(result);
    }

    /**
     * Lay ket qua cho bulk operation va chan tap ket qua vuot gioi han.
     */
    public List<Question> findForBulk(
            UUID subjectId,
            UUID teacherId,
            QuestionSearchCriteria criteria,
            int limit
    ) {
        validateReferences(subjectId, teacherId, criteria.topicId(), criteria.collectionId());
        Page<Question> page = questionRepo.findAll(
                QuestionSpecifications.accessible(subjectId, teacherId, criteria),
                PageRequest.of(0, limit + 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        if (page.getTotalElements() > limit) {
            throw error(HttpStatus.BAD_REQUEST, "FILTER_RESULT_LIMIT_EXCEEDED");
        }
        return page.getContent();
    }

    public QuestionSearchCriteria criteria(
            UUID subjectId,
            UUID teacherId,
            String keyword,
            UUID topicId,
            String difficulty,
            String visibility,
            String ownerScope,
            String questionType,
            UUID collectionId,
            String membership,
            List<UUID> excludeQuestionIds
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        validateReferences(subjectId, teacherId, topicId, collectionId);
        return new QuestionSearchCriteria(
                keyword,
                topicId,
                parseList(difficulty, Difficulty.class),
                parseList(visibility, QuestionVisibility.class),
                parse(ownerScope, OwnershipScope.class, OwnershipScope.ALL),
                normalizeQuestionType(questionType),
                collectionId,
                parse(membership, CollectionMembership.class, CollectionMembership.ALL),
                excludeQuestionIds
        );
    }

    private void validateReferences(UUID subjectId, UUID teacherId, UUID topicId, UUID collectionId) {
        if (topicId != null && !topicRepo.existsByIdAndSubjectId(topicId, subjectId)) {
            throw error(HttpStatus.NOT_FOUND, "TOPIC_NOT_FOUND");
        }
        if (collectionId != null) {
            QuestionCollection collection = collectionRepo.findByIdAndSubjectId(collectionId, subjectId)
                    .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND"));
            if (!collection.getOwnerTeacherId().equals(teacherId)
                    && collection.getVisibility() != CollectionVisibility.PUBLIC) {
                // Dung NOT_FOUND de khong lam lo bo cau hoi rieng tu cua giao vien khac.
                throw error(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND");
            }
        }
    }

    private org.springframework.data.domain.Pageable pageable(int page, int size, String sortValue) {
        if (page < 0 || size < 1 || size > 1000) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        }
        String[] parts = sortValue == null || sortValue.isBlank()
                ? new String[]{"createdAt", "desc"}
                : sortValue.split(",", 2);
        String field = parts[0];
        if (!SORT_FIELDS.contains(field)) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SORT_FIELD");
        }
        Sort.Direction direction = parts.length == 2
                ? parseDirection(parts[1])
                : Sort.Direction.ASC;
        return PageRequest.of(page, size, Sort.by(direction, field));
    }

    private Sort.Direction parseDirection(String value) {
        try {
            return Sort.Direction.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SORT_DIRECTION");
        }
    }

    private String normalizeQuestionType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (!Set.of("SINGLE_CHOICE", "MULTI_CHOICE").contains(normalized)) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_QUESTION_TYPE");
        }
        return normalized;
    }

    private <T extends Enum<T>> List<T> parseList(String value, Class<T> enumType) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        try {
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(item -> !item.isBlank())
                    .map(item -> Enum.valueOf(enumType, item.toUpperCase(Locale.ROOT)))
                    .toList();
        } catch (IllegalArgumentException exception) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILTER_VALUE");
        }
    }

    private <T extends Enum<T>> T parse(String value, Class<T> enumType, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILTER_VALUE");
        }
    }

    private QuestionResponseDTO toResponse(Question question) {
        return new QuestionResponseDTO(
                question.getId(),
                question.getSubjectId(),
                question.getTopicId(),
                question.getOwnerTeacherId(),
                question.getQuestionType(),
                question.getContent(),
                question.getDifficulty(),
                question.getDefaultScore(),
                question.getEstimatedSecond(),
                question.getVisibility(),
                imageObjectKey(question.getMetadata()),
                question.getStatus(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }

    private String imageObjectKey(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            Map<String, Object> value = objectMapper.readValue(
                    metadata,
                    new TypeReference<Map<String, Object>>() {}
            );
            Object imageObjectKey = value.get("imageObjectKey");
            if (!(imageObjectKey instanceof String text)) {
                return null;
            }
            return text.startsWith("questions/") && !text.contains("..") ? text : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
