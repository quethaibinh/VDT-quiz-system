package com.question_service.question_service.service.collections;

import com.question_service.question_service.model.dto.collections.*;
import com.question_service.question_service.model.dto.common.PageResponseDTO;
import com.question_service.question_service.model.entity.*;
import com.question_service.question_service.repository.CollectionDifficultyCount;
import com.question_service.question_service.repository.QuestionCollectionItemRepo;
import com.question_service.question_service.repository.QuestionCollectionRepo;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
/**
 * Quan ly thong tin, quyen xem va vong doi cua bo cau hoi.
 */
public class QuestionCollectionService {

    private static final Set<String> SORT_FIELDS = Set.of("createdAt", "updatedAt", "name");

    private final QuestionCollectionRepo collectionRepo;
    private final QuestionCollectionItemRepo itemRepo;
    private final TeacherSubjectAccessService subjectAccessService;

    public QuestionCollectionService(
            QuestionCollectionRepo collectionRepo,
            QuestionCollectionItemRepo itemRepo,
            TeacherSubjectAccessService subjectAccessService
    ) {
        this.collectionRepo = collectionRepo;
        this.itemRepo = itemRepo;
        this.subjectAccessService = subjectAccessService;
    }

    @Transactional
    /**
     * Tao bo cau hoi rieng cho giao vien trong mon hoc duoc phan cong.
     */
    public CollectionResponseDTO create(
            UUID subjectId,
            UUID teacherId,
            CollectionRequestDTO request
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        String name = requireName(request.name());
        if (collectionRepo.existsByOwnerTeacherIdAndSubjectIdAndNameIgnoreCase(
                teacherId, subjectId, name
        )) {
            throw error(HttpStatus.CONFLICT, "DUPLICATE_COLLECTION_NAME");
        }

        QuestionCollection collection = new QuestionCollection();
        collection.setSubjectId(subjectId);
        collection.setOwnerTeacherId(teacherId);
        collection.setName(name);
        collection.setDescription(trimToNull(request.description()));
        collection.setVisibility(parseVisibility(request.visibility()));
        collection.setStatus(CollectionStatus.ACTIVE);
        return toResponse(collectionRepo.save(collection), teacherId);
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<CollectionResponseDTO> list(
            UUID subjectId,
            UUID teacherId,
            String visibility,
            String status,
            String ownership,
            String keyword,
            int page,
            int size,
            String sort
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        Page<CollectionResponseDTO> result = collectionRepo.findAll(
                        CollectionSpecifications.visibleToTeacher(
                                subjectId,
                                teacherId,
                                parseOptional(visibility, CollectionVisibility.class),
                                parse(status, CollectionStatus.class, CollectionStatus.ACTIVE),
                                parse(ownership, OwnershipScope.class, OwnershipScope.ALL),
                                keyword
                        ),
                        pageable(page, size, sort)
                )
                .map(collection -> toResponse(collection, teacherId));
        return PageResponseDTO.from(result);
    }

    @Transactional(readOnly = true)
    public CollectionResponseDTO get(UUID subjectId, UUID collectionId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        return toResponse(loadReadable(subjectId, collectionId, teacherId), teacherId);
    }

    @Transactional
    public CollectionResponseDTO update(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId,
            CollectionRequestDTO request
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        QuestionCollection collection = loadOwnedActive(subjectId, collectionId, teacherId);
        String name = requireName(request.name());
        if (collectionRepo.existsByOwnerTeacherIdAndSubjectIdAndNameIgnoreCaseAndIdNot(
                teacherId, subjectId, name, collectionId
        )) {
            throw error(HttpStatus.CONFLICT, "DUPLICATE_COLLECTION_NAME");
        }

        CollectionVisibility targetVisibility = parseVisibility(request.visibility());
        // Bo cong khai khong duoc chua cau hoi rieng tu.
        if (targetVisibility == CollectionVisibility.PUBLIC
                && itemRepo.countPrivateQuestions(collectionId) > 0) {
            throw error(HttpStatus.BAD_REQUEST,
                    "PRIVATE_QUESTION_NOT_ALLOWED_IN_PUBLIC_COLLECTION");
        }

        collection.setName(name);
        collection.setDescription(trimToNull(request.description()));
        collection.setVisibility(targetVisibility);
        return toResponse(collectionRepo.save(collection), teacherId);
    }

    @Transactional
    public CollectionResponseDTO archive(UUID subjectId, UUID collectionId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        QuestionCollection collection = loadOwned(subjectId, collectionId, teacherId);
        collection.setStatus(CollectionStatus.ARCHIVED);
        collection.setDeletedAt(java.time.LocalDateTime.now());
        return toResponse(collectionRepo.save(collection), teacherId);
    }

    @Transactional
    public CollectionResponseDTO restore(UUID subjectId, UUID collectionId, UUID teacherId) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        QuestionCollection collection = loadOwned(subjectId, collectionId, teacherId);
        if (collection.getVisibility() == CollectionVisibility.PUBLIC
                && itemRepo.countPrivateQuestions(collectionId) > 0) {
            throw error(HttpStatus.BAD_REQUEST,
                    "PRIVATE_QUESTION_NOT_ALLOWED_IN_PUBLIC_COLLECTION");
        }
        collection.setStatus(CollectionStatus.ACTIVE);
        collection.setDeletedAt(null);
        return toResponse(collectionRepo.save(collection), teacherId);
    }

    public QuestionCollection loadReadable(UUID subjectId, UUID collectionId, UUID teacherId) {
        QuestionCollection collection = collectionRepo.findByIdAndSubjectId(collectionId, subjectId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND"));
        if (!collection.getOwnerTeacherId().equals(teacherId)
                && collection.getVisibility() != CollectionVisibility.PUBLIC) {
            // An su ton tai cua bo rieng tu doi voi nguoi khong co quyen.
            throw error(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND");
        }
        return collection;
    }

    public QuestionCollection loadOwnedActive(UUID subjectId, UUID collectionId, UUID teacherId) {
        QuestionCollection collection = loadOwned(subjectId, collectionId, teacherId);
        if (collection.getStatus() != CollectionStatus.ACTIVE) {
            throw error(HttpStatus.BAD_REQUEST, "COLLECTION_ARCHIVED");
        }
        return collection;
    }

    private QuestionCollection loadOwned(UUID subjectId, UUID collectionId, UUID teacherId) {
        QuestionCollection collection = collectionRepo.findByIdAndSubjectId(collectionId, subjectId)
                .orElseThrow(() -> error(HttpStatus.NOT_FOUND, "COLLECTION_NOT_FOUND"));
        if (!collection.getOwnerTeacherId().equals(teacherId)) {
            throw error(HttpStatus.FORBIDDEN, "COLLECTION_FORBIDDEN");
        }
        return collection;
    }

    public CollectionResponseDTO toResponse(QuestionCollection collection, UUID teacherId) {
        return new CollectionResponseDTO(
                collection.getId(),
                collection.getSubjectId(),
                collection.getOwnerTeacherId(),
                collection.getName(),
                collection.getDescription(),
                collection.getVisibility(),
                collection.getStatus(),
                collection.getOwnerTeacherId().equals(teacherId)
                        && collection.getStatus() == CollectionStatus.ACTIVE,
                stats(collection.getId()),
                collection.getCreatedAt(),
                collection.getUpdatedAt()
        );
    }

    public CollectionStatsDTO stats(UUID collectionId) {
        long easy = 0;
        long medium = 0;
        long hard = 0;
        List<CollectionDifficultyCount> counts = itemRepo.countByDifficulty(collectionId);
        for (CollectionDifficultyCount count : counts) {
            switch (count.getDifficulty()) {
                case EASY -> easy = count.getCount();
                case MEDIUM -> medium = count.getCount();
                case HARD -> hard = count.getCount();
            }
        }
        return new CollectionStatsDTO(itemRepo.countByCollectionId(collectionId), easy, medium, hard);
    }

    private org.springframework.data.domain.Pageable pageable(int page, int size, String sortValue) {
        if (page < 0 || size < 1 || size > 100) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_PAGE_REQUEST");
        }
        String[] parts = sortValue == null || sortValue.isBlank()
                ? new String[]{"updatedAt", "desc"}
                : sortValue.split(",", 2);
        if (!SORT_FIELDS.contains(parts[0])) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SORT_FIELD");
        }
        Sort.Direction direction;
        try {
            direction = parts.length == 2
                    ? Sort.Direction.fromString(parts[1])
                    : Sort.Direction.ASC;
        } catch (IllegalArgumentException exception) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_SORT_DIRECTION");
        }
        return PageRequest.of(page, size, Sort.by(direction, parts[0]));
    }

    private String requireName(String name) {
        String value = trimToNull(name);
        if (value == null) {
            throw error(HttpStatus.BAD_REQUEST, "REQUIRED_FIELD:name");
        }
        return value;
    }

    private CollectionVisibility parseVisibility(String value) {
        if (value == null || value.isBlank()) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_COLLECTION_VISIBILITY");
        }
        return parse(value, CollectionVisibility.class, null);
    }

    private <T extends Enum<T>> T parseOptional(String value, Class<T> type) {
        return parse(value, type, null);
    }

    private <T extends Enum<T>> T parse(String value, Class<T> type, T defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw error(HttpStatus.BAD_REQUEST, "INVALID_FILTER_VALUE");
        }
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
