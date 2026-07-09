package com.question_service.question_service.service.collections;

import com.question_service.question_service.model.dto.collections.*;
import com.question_service.question_service.model.dto.common.PageResponseDTO;
import com.question_service.question_service.model.dto.questions.QuestionResponseDTO;
import com.question_service.question_service.model.entity.*;
import com.question_service.question_service.model.entity.enums.CollectionMembership;
import com.question_service.question_service.model.entity.enums.CollectionVisibility;
import com.question_service.question_service.model.entity.enums.QuestionStatus;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import com.question_service.question_service.repository.QuestionCollectionItemRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.service.questions.QuestionSearchCriteria;
import com.question_service.question_service.service.questions.TeacherQuestionSearchService;
import com.question_service.question_service.service.subjects.TeacherSubjectAccessService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@Service
/**
 * Them, xoa va liet ke cau hoi trong mot bo cau hoi.
 */
public class QuestionCollectionItemService {

    private static final int FILTER_LIMIT = 1000;

    private final QuestionCollectionItemRepo itemRepo;
    private final QuestionRepo questionRepo;
    private final QuestionCollectionService collectionService;
    private final TeacherQuestionSearchService questionSearchService;
    private final TeacherSubjectAccessService subjectAccessService;

    public QuestionCollectionItemService(
            QuestionCollectionItemRepo itemRepo,
            QuestionRepo questionRepo,
            QuestionCollectionService collectionService,
            TeacherQuestionSearchService questionSearchService,
            TeacherSubjectAccessService subjectAccessService
    ) {
        this.itemRepo = itemRepo;
        this.questionRepo = questionRepo;
        this.collectionService = collectionService;
        this.questionSearchService = questionSearchService;
        this.subjectAccessService = subjectAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponseDTO<QuestionResponseDTO> listQuestions(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId,
            String keyword,
            UUID topicId,
            String difficulty,
            String visibility,
            String ownerScope,
            String questionType,
            int page,
            int size,
            String sort
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        collectionService.loadReadable(subjectId, collectionId, teacherId);
        return questionSearchService.search(
                subjectId,
                teacherId,
                keyword,
                topicId,
                difficulty,
                visibility,
                ownerScope,
                questionType,
                collectionId,
                CollectionMembership.IN.name(),
                page,
                size,
                sort
        );
    }

    @Transactional
    /**
     * Them cac cau hoi hop le va bo qua cau hoi da co trong bo.
     */
    public BulkCollectionResultDTO add(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId,
            List<UUID> requestedIds
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        QuestionCollection collection = collectionService.loadOwnedActive(
                subjectId, collectionId, teacherId
        );
        List<UUID> ids = normalizeIds(requestedIds);
        List<Question> questions = loadAndValidateQuestions(subjectId, teacherId, collection, ids);
        Set<UUID> existingIds = itemRepo.findQuestionIds(collectionId);
        List<QuestionCollectionItem> additions = questions.stream()
                .filter(question -> !existingIds.contains(question.getId()))
                .map(question -> item(collectionId, question.getId()))
                .toList();
        itemRepo.saveAll(additions);
        return result(ids.size(), questions.size(), additions.size(), 0,
                ids.size() - additions.size(), collectionId);
    }

    @Transactional
    public BulkCollectionResultDTO remove(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId,
            List<UUID> requestedIds
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        collectionService.loadOwnedActive(subjectId, collectionId, teacherId);
        List<UUID> ids = normalizeIds(requestedIds);
        int removed = itemRepo.deleteItems(collectionId, ids);
        return result(ids.size(), ids.size(), 0, removed,
                ids.size() - removed, collectionId);
    }

    @Transactional
    /**
     * Them theo bo loc nhung gioi han ket qua de tranh xu ly hang loat qua lon.
     */
    public BulkCollectionResultDTO addByFilter(
            UUID subjectId,
            UUID collectionId,
            UUID teacherId,
            AddByFilterRequestDTO request
    ) {
        subjectAccessService.requireActiveAssignment(subjectId, teacherId);
        QuestionCollection collection = collectionService.loadOwnedActive(
                subjectId, collectionId, teacherId
        );
        QuestionSearchCriteria criteria = questionSearchService.criteria(
                subjectId,
                teacherId,
                request.keyword(),
                request.topicId(),
                join(request.difficulties()),
                join(request.visibilities()),
                request.ownerScope(),
                request.questionType(),
                null,
                CollectionMembership.ALL.name(),
                request.excludeQuestionIds()
        );
        List<Question> questions = questionSearchService.findForBulk(
                subjectId, teacherId, criteria, FILTER_LIMIT
        );
        validateQuestionRules(subjectId, teacherId, collection, questions);
        Set<UUID> existingIds = itemRepo.findQuestionIds(collectionId);
        List<QuestionCollectionItem> additions = questions.stream()
                .filter(question -> !existingIds.contains(question.getId()))
                .map(question -> item(collectionId, question.getId()))
                .toList();
        itemRepo.saveAll(additions);
        return result(questions.size(), questions.size(), additions.size(), 0,
                questions.size() - additions.size(), collectionId);
    }

    private List<Question> loadAndValidateQuestions(
            UUID subjectId,
            UUID teacherId,
            QuestionCollection collection,
            List<UUID> ids
    ) {
        List<Question> questions = questionRepo.findAllById(ids);
        if (questions.size() != ids.size()) {
            throw error(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND");
        }
        validateQuestionRules(subjectId, teacherId, collection, questions);
        return questions;
    }

    private void validateQuestionRules(
            UUID subjectId,
            UUID teacherId,
            QuestionCollection collection,
            List<Question> questions
    ) {
        for (Question question : questions) {
            if (!subjectId.equals(question.getSubjectId())) {
                throw error(HttpStatus.BAD_REQUEST, "QUESTION_SUBJECT_MISMATCH");
            }
            if (question.getStatus() != QuestionStatus.ACTIVE) {
                throw error(HttpStatus.BAD_REQUEST, "QUESTION_ARCHIVED");
            }
            boolean accessible = question.getVisibility() == QuestionVisibility.PUBLIC
                    || (question.getVisibility() == QuestionVisibility.PRIVATE
                    && teacherId.equals(question.getOwnerTeacherId()));
            if (!accessible) {
                throw error(HttpStatus.FORBIDDEN, "QUESTION_FORBIDDEN");
            }
            if (collection.getVisibility() == CollectionVisibility.PUBLIC
                    && question.getVisibility() != QuestionVisibility.PUBLIC) {
                // Giu tinh nhat quan: bo cong khai chi chua cau hoi cong khai.
                throw error(HttpStatus.BAD_REQUEST,
                        "PRIVATE_QUESTION_NOT_ALLOWED_IN_PUBLIC_COLLECTION");
            }
        }
    }

    private List<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            throw error(HttpStatus.BAD_REQUEST, "QUESTION_IDS_REQUIRED");
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    private BulkCollectionResultDTO result(
            int requested,
            int matched,
            int added,
            int removed,
            int skipped,
            UUID collectionId
    ) {
        return new BulkCollectionResultDTO(
                requested,
                matched,
                added,
                removed,
                skipped,
                collectionService.stats(collectionId).questionCount()
        );
    }

    private QuestionCollectionItem item(UUID collectionId, UUID questionId) {
        QuestionCollectionItem item = new QuestionCollectionItem();
        item.setCollectionId(collectionId);
        item.setQuestionId(questionId);
        return item;
    }

    private String join(List<String> values) {
        return values == null || values.isEmpty() ? null : String.join(",", values);
    }

    private ResponseStatusException error(HttpStatus status, String code) {
        return new ResponseStatusException(status, code);
    }
}
