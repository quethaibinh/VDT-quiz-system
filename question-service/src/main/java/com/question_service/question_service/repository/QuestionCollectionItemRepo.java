package com.question_service.question_service.repository;

import com.question_service.question_service.model.entity.QuestionCollectionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface QuestionCollectionItemRepo extends JpaRepository<QuestionCollectionItem, UUID> {

    @Query("select i.questionId from QuestionCollectionItem i where i.collectionId = :collectionId")
    Set<UUID> findQuestionIds(@Param("collectionId") UUID collectionId);

    @Modifying
    @Query("""
            delete from QuestionCollectionItem i
            where i.collectionId = :collectionId
              and i.questionId in :questionIds
            """)
    int deleteItems(
            @Param("collectionId") UUID collectionId,
            @Param("questionIds") Collection<UUID> questionIds
    );

    long countByCollectionId(UUID collectionId);

    @Query("""
            select count(q)
            from QuestionCollectionItem i
            join Question q on q.id = i.questionId
            where i.collectionId = :collectionId
              and q.visibility = com.question_service.question_service.model.entity.QuestionVisibility.PRIVATE
            """)
    long countPrivateQuestions(@Param("collectionId") UUID collectionId);

    @Query("""
            select q.difficulty as difficulty, count(q) as count
            from QuestionCollectionItem i
            join Question q on q.id = i.questionId
            where i.collectionId = :collectionId
            group by q.difficulty
            """)
    List<CollectionDifficultyCount> countByDifficulty(@Param("collectionId") UUID collectionId);

}
