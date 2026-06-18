package com.question_service.question_service.service.collections;

import com.question_service.question_service.model.dto.collections.*;
import com.question_service.question_service.model.entity.*;
import com.question_service.question_service.model.entity.enums.CollectionStatus;
import com.question_service.question_service.model.entity.enums.Difficulty;
import com.question_service.question_service.model.entity.enums.QuestionStatus;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import com.question_service.question_service.repository.*;
import com.question_service.question_service.service.questions.TeacherQuestionSearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class QuestionCollectionIntegrationTests {

    @Autowired
    private QuestionCollectionService collectionService;
    @Autowired
    private QuestionCollectionItemService itemService;
    @Autowired
    private TeacherQuestionSearchService searchService;
    @Autowired
    private SubjectRepo subjectRepo;
    @Autowired
    private SubjectTeacherRepo subjectTeacherRepo;
    @Autowired
    private TopicRepo topicRepo;
    @Autowired
    private QuestionRepo questionRepo;

    private UUID subjectId;
    private UUID teacherId;
    private UUID otherTeacherId;
    private UUID topicId;

    @BeforeEach
    void setUp() {
        teacherId = UUID.randomUUID();
        otherTeacherId = UUID.randomUUID();

        Subject subject = new Subject();
        subject.setCode("SUB-" + UUID.randomUUID());
        subject.setName("Subject");
        subject.setStatus(SubjectStatus.ACTIVE);
        subjectId = subjectRepo.save(subject).getId();

        assign(teacherId);
        assign(otherTeacherId);

        Topic topic = new Topic();
        topic.setSubjectId(subjectId);
        topic.setName("Topic");
        topic.setSlug("topic-" + UUID.randomUUID());
        topicId = topicRepo.save(topic).getId();
    }

    @Test
    void privateCollectionIsVisibleOnlyToOwnerAndPublicCollectionIsShared() {
        CollectionResponseDTO privateCollection = create("Private", "PRIVATE");
        CollectionResponseDTO publicCollection = create("Public", "PUBLIC");

        assertThat(collectionService.list(
                subjectId, otherTeacherId, null, null, null, null,
                0, 20, "updatedAt,desc"
        ).content())
                .extracting(CollectionResponseDTO::id)
                .contains(publicCollection.id())
                .doesNotContain(privateCollection.id());

        assertThatThrownBy(() -> collectionService.get(
                subjectId, privateCollection.id(), otherTeacherId
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("COLLECTION_NOT_FOUND");
    }

    @Test
    void publicCollectionRejectsPrivateQuestionAtomically() {
        CollectionResponseDTO collection = create("Public", "PUBLIC");
        Question publicQuestion = question(teacherId, QuestionVisibility.PUBLIC, Difficulty.EASY);
        Question privateQuestion = question(teacherId, QuestionVisibility.PRIVATE, Difficulty.HARD);

        assertThatThrownBy(() -> itemService.add(
                subjectId,
                collection.id(),
                teacherId,
                List.of(publicQuestion.getId(), privateQuestion.getId())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("PRIVATE_QUESTION_NOT_ALLOWED_IN_PUBLIC_COLLECTION");

        assertThat(collectionService.get(subjectId, collection.id(), teacherId)
                .stats().questionCount()).isZero();
    }

    @Test
    void privateCollectionAcceptsPublicAndOwnedPrivateQuestionsAndReportsStats() {
        CollectionResponseDTO collection = create("Private", "PRIVATE");
        Question easy = question(otherTeacherId, QuestionVisibility.PUBLIC, Difficulty.EASY);
        Question hard = question(teacherId, QuestionVisibility.PRIVATE, Difficulty.HARD);

        BulkCollectionResultDTO result = itemService.add(
                subjectId, collection.id(), teacherId, List.of(easy.getId(), hard.getId())
        );

        assertThat(result.addedCount()).isEqualTo(2);
        CollectionStatsDTO stats = collectionService.get(
                subjectId, collection.id(), teacherId
        ).stats();
        assertThat(stats.questionCount()).isEqualTo(2);
        assertThat(stats.easy()).isEqualTo(1);
        assertThat(stats.hard()).isEqualTo(1);
    }

    @Test
    void searchAppliesVisibilityAndMembershipFilters() {
        CollectionResponseDTO collection = create("Pool", "PRIVATE");
        Question shared = question(otherTeacherId, QuestionVisibility.PUBLIC, Difficulty.MEDIUM);
        Question ownedPrivate = question(teacherId, QuestionVisibility.PRIVATE, Difficulty.MEDIUM);
        question(otherTeacherId, QuestionVisibility.PRIVATE, Difficulty.MEDIUM);
        itemService.add(subjectId, collection.id(), teacherId, List.of(shared.getId()));

        var inCollection = searchService.search(
                subjectId, teacherId, null, topicId, "MEDIUM", null,
                "ALL", null, collection.id(), "IN", 0, 20, "createdAt,desc"
        );
        var notInCollection = searchService.search(
                subjectId, teacherId, null, topicId, "MEDIUM", null,
                "ALL", null, collection.id(), "NOT_IN", 0, 20, "createdAt,desc"
        );

        assertThat(inCollection.content())
                .extracting(value -> value.id())
                .containsExactly(shared.getId());
        assertThat(notInCollection.content())
                .extracting(value -> value.id())
                .contains(ownedPrivate.getId())
                .doesNotContain(shared.getId());
        assertThat(notInCollection.totalElements()).isEqualTo(1);
    }

    @Test
    void collectionQuestionListSupportsQuestionFilters() {
        CollectionResponseDTO collection = create("Filtered pool", "PRIVATE");
        Question easy = question(teacherId, QuestionVisibility.PUBLIC, Difficulty.EASY);
        Question hard = question(teacherId, QuestionVisibility.PUBLIC, Difficulty.HARD);
        itemService.add(
                subjectId,
                collection.id(),
                teacherId,
                List.of(easy.getId(), hard.getId())
        );

        var result = itemService.listQuestions(
                subjectId,
                collection.id(),
                teacherId,
                "Question",
                topicId,
                "HARD",
                "PUBLIC",
                "MINE",
                "SINGLE_CHOICE",
                0,
                20,
                "createdAt,desc"
        );

        assertThat(result.content())
                .extracting(value -> value.id())
                .containsExactly(hard.getId());
    }

    @Test
    void archivedCollectionCannotBeMutatedAndCanBeRestored() {
        CollectionResponseDTO collection = create("Archive", "PRIVATE");
        collectionService.archive(subjectId, collection.id(), teacherId);

        assertThatThrownBy(() -> itemService.add(
                subjectId,
                collection.id(),
                teacherId,
                List.of(question(teacherId, QuestionVisibility.PUBLIC, Difficulty.EASY).getId())
        ))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("COLLECTION_ARCHIVED");

        assertThat(collectionService.restore(subjectId, collection.id(), teacherId).status())
                .isEqualTo(CollectionStatus.ACTIVE);
    }

    private CollectionResponseDTO create(String name, String visibility) {
        return collectionService.create(
                subjectId,
                teacherId,
                new CollectionRequestDTO(name + "-" + UUID.randomUUID(), null, visibility)
        );
    }

    private Question question(UUID ownerId, QuestionVisibility visibility, Difficulty difficulty) {
        Question question = new Question();
        question.setSubjectId(subjectId);
        question.setTopicId(topicId);
        question.setOwnerTeacherId(ownerId);
        question.setQuestionType("SINGLE_CHOICE");
        question.setContent("Question " + UUID.randomUUID());
        question.setDifficulty(difficulty);
        question.setDefaultScore(1.0);
        question.setVisibility(visibility);
        question.setStatus(QuestionStatus.ACTIVE);
        return questionRepo.save(question);
    }

    private void assign(UUID assignedTeacherId) {
        SubjectTeacher assignment = new SubjectTeacher();
        assignment.setSubjectId(subjectId);
        assignment.setTeacherId(assignedTeacherId);
        assignment.setStatus(SubjectTeacherStatus.ACTIVE);
        subjectTeacherRepo.save(assignment);
    }
}
