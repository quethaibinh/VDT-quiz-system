package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.entity.CollectionMembership;
import com.question_service.question_service.model.entity.Difficulty;
import com.question_service.question_service.model.entity.OwnershipScope;
import com.question_service.question_service.model.entity.QuestionVisibility;

import java.util.List;
import java.util.UUID;

public record QuestionSearchCriteria(
        String keyword,
        UUID topicId,
        List<Difficulty> difficulties,
        List<QuestionVisibility> visibilities,
        OwnershipScope ownerScope,
        String questionType,
        UUID collectionId,
        CollectionMembership membership,
        List<UUID> excludeQuestionIds
) {
}
