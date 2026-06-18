package com.question_service.question_service.service.collections;

import com.question_service.question_service.model.entity.CollectionStatus;
import com.question_service.question_service.model.entity.CollectionVisibility;
import com.question_service.question_service.model.entity.OwnershipScope;
import com.question_service.question_service.model.entity.QuestionCollection;
import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.UUID;

final class CollectionSpecifications {

    private CollectionSpecifications() {
    }

    /**
     * Tao truy van dong theo quyen so huu, visibility, trang thai va tu khoa.
     */
    static Specification<QuestionCollection> visibleToTeacher(
            UUID subjectId,
            UUID teacherId,
            CollectionVisibility visibility,
            CollectionStatus status,
            OwnershipScope ownership,
            String keyword
    ) {
        return (root, query, cb) -> {
            var predicate = cb.equal(root.get("subjectId"), subjectId);

            predicate = cb.and(predicate, switch (ownership) {
                case MINE -> cb.equal(root.get("ownerTeacherId"), teacherId);
                case SHARED -> cb.and(
                        cb.notEqual(root.get("ownerTeacherId"), teacherId),
                        cb.equal(root.get("visibility"), CollectionVisibility.PUBLIC)
                );
                case ALL -> cb.or(
                        cb.equal(root.get("ownerTeacherId"), teacherId),
                        cb.equal(root.get("visibility"), CollectionVisibility.PUBLIC)
                );
            });

            if (visibility != null) {
                predicate = cb.and(predicate, cb.equal(root.get("visibility"), visibility));
            }
            if (status != null) {
                predicate = cb.and(predicate, cb.equal(root.get("status"), status));
            }
            if (keyword != null && !keyword.isBlank()) {
                String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
                predicate = cb.and(predicate, cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern)
                ));
            }
            return predicate;
        };
    }
}
