package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.entity.*;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

public final class QuestionSpecifications {

    private QuestionSpecifications() {
    }

    /**
     * Tao truy van dong chi gom cau hoi cong khai hoac cau hoi rieng cua giao vien.
     */
    public static Specification<Question> accessible(
            UUID subjectId,
            UUID teacherId,
            QuestionSearchCriteria criteria
    ) {
        return (root, query, cb) -> {
            var predicate = cb.and(
                    cb.equal(root.get("subjectId"), subjectId),
                    cb.equal(root.get("status"), QuestionStatus.ACTIVE)
            );

            var access = cb.or(
                    cb.equal(root.get("visibility"), QuestionVisibility.PUBLIC),
                    cb.and(
                            cb.equal(root.get("visibility"), QuestionVisibility.PRIVATE),
                            cb.equal(root.get("ownerTeacherId"), teacherId)
                    )
            );
            predicate = cb.and(predicate, access);

            OwnershipScope ownerScope = criteria.ownerScope() == null
                    ? OwnershipScope.ALL
                    : criteria.ownerScope();
            if (ownerScope == OwnershipScope.MINE) {
                predicate = cb.and(predicate, cb.equal(root.get("ownerTeacherId"), teacherId));
            } else if (ownerScope == OwnershipScope.SHARED) {
                predicate = cb.and(predicate,
                        cb.notEqual(root.get("ownerTeacherId"), teacherId),
                        cb.equal(root.get("visibility"), QuestionVisibility.PUBLIC));
            }

            if (criteria.keyword() != null && !criteria.keyword().isBlank()) {
                String pattern = "%" + criteria.keyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.like(cb.lower(root.get("content")), pattern));
            }
            if (criteria.topicId() != null) {
                predicate = cb.and(predicate, cb.equal(root.get("topicId"), criteria.topicId()));
            }
            if (criteria.difficulties() != null && !criteria.difficulties().isEmpty()) {
                predicate = cb.and(predicate, root.get("difficulty").in(criteria.difficulties()));
            }
            if (criteria.visibilities() != null && !criteria.visibilities().isEmpty()) {
                predicate = cb.and(predicate, root.get("visibility").in(criteria.visibilities()));
            }
            if (criteria.questionType() != null && !criteria.questionType().isBlank()) {
                predicate = cb.and(predicate,
                        cb.equal(root.get("questionType"), criteria.questionType().trim().toUpperCase(Locale.ROOT)));
            }
            if (criteria.excludeQuestionIds() != null && !criteria.excludeQuestionIds().isEmpty()) {
                predicate = cb.and(predicate, cb.not(root.get("id").in(criteria.excludeQuestionIds())));
            }

            CollectionMembership membership = criteria.membership() == null
                    ? CollectionMembership.ALL
                    : criteria.membership();
            if (criteria.collectionId() != null && membership != CollectionMembership.ALL) {
                // Truy van con loc cau hoi dua tren viec co nam trong bo cau hoi hay khong.
                Subquery<UUID> subquery = query.subquery(UUID.class);
                Root<QuestionCollectionItem> item = subquery.from(QuestionCollectionItem.class);
                subquery.select(item.get("questionId"))
                        .where(cb.equal(item.get("collectionId"), criteria.collectionId()));
                predicate = membership == CollectionMembership.IN
                        ? cb.and(predicate, root.get("id").in(subquery))
                        : cb.and(predicate, cb.not(root.get("id").in(subquery)));
            }

            return predicate;
        };
    }
}
