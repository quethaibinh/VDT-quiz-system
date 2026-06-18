package com.question_service.question_service.service.questions;

import com.question_service.question_service.model.dto.questions.QuestionOptionRequestDTO;
import com.question_service.question_service.model.dto.questions.QuestionUpsertRequestDTO;
import com.question_service.question_service.model.dto.collections.CollectionRequestDTO;
import com.question_service.question_service.model.entity.QuestionStatus;
import com.question_service.question_service.model.entity.Source;
import com.question_service.question_service.model.entity.Subject;
import com.question_service.question_service.model.entity.SubjectStatus;
import com.question_service.question_service.model.entity.SubjectTeacher;
import com.question_service.question_service.model.entity.SubjectTeacherStatus;
import com.question_service.question_service.model.entity.Topic;
import com.question_service.question_service.repository.QuestionCollectionItemRepo;
import com.question_service.question_service.repository.QuestionOptionRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.repository.SubjectRepo;
import com.question_service.question_service.repository.SubjectTeacherRepo;
import com.question_service.question_service.repository.TopicRepo;
import com.question_service.question_service.service.collections.QuestionCollectionItemService;
import com.question_service.question_service.service.collections.QuestionCollectionService;
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
class TeacherQuestionCrudServiceTests {

    @Autowired
    private TeacherQuestionCrudService service;
    @Autowired
    private SubjectRepo subjectRepo;
    @Autowired
    private SubjectTeacherRepo subjectTeacherRepo;
    @Autowired
    private TopicRepo topicRepo;
    @Autowired
    private QuestionRepo questionRepo;
    @Autowired
    private QuestionOptionRepo optionRepo;
    @Autowired
    private QuestionCollectionItemRepo collectionItemRepo;
    @Autowired
    private QuestionCollectionService collectionService;
    @Autowired
    private QuestionCollectionItemService collectionItemService;

    private UUID subjectId;
    private UUID topicId;
    private UUID ownerId;
    private UUID otherTeacherId;

    @BeforeEach
    void setUp() {
        ownerId = UUID.randomUUID();
        otherTeacherId = UUID.randomUUID();

        Subject subject = new Subject();
        subject.setCode("CRUD-" + UUID.randomUUID());
        subject.setName("Question CRUD");
        subject.setStatus(SubjectStatus.ACTIVE);
        subjectId = subjectRepo.save(subject).getId();
        assign(ownerId);
        assign(otherTeacherId);

        Topic topic = new Topic();
        topic.setSubjectId(subjectId);
        topic.setName("Logic");
        topic.setSlug("logic-" + UUID.randomUUID());
        topicId = topicRepo.save(topic).getId();
    }

    @Test
    void createsManualActiveQuestionAndReturnsOwnerAnswers() {
        var result = service.create(subjectId, ownerId, request("PRIVATE", "Question one"));

        assertThat(result.ownerTeacherId()).isEqualTo(ownerId);
        assertThat(result.source()).isEqualTo(Source.MANUAL);
        assertThat(result.status()).isEqualTo(QuestionStatus.ACTIVE);
        assertThat(result.options()).extracting(value -> value.correct())
                .containsExactly(true, false);
    }

    @Test
    void publicDetailForOtherTeacherHidesAnswersAndExplanation() {
        var created = service.create(subjectId, ownerId, request("PUBLIC", "Shared"));

        var shared = service.getDetail(subjectId, created.id(), otherTeacherId);

        assertThat(shared.explanation()).isNull();
        assertThat(shared.options()).allSatisfy(option -> {
            assertThat(option.correct()).isNull();
            assertThat(option.explanation()).isNull();
        });
    }

    @Test
    void privateDetailForOtherTeacherIsNotFound() {
        var created = service.create(subjectId, ownerId, request("PRIVATE", "Private"));

        assertThatThrownBy(() -> service.getDetail(subjectId, created.id(), otherTeacherId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("QUESTION_NOT_FOUND");
    }

    @Test
    void updateReplacesMutableFieldsAndOptions() {
        var created = service.create(subjectId, ownerId, request("PRIVATE", "Before"));
        QuestionUpsertRequestDTO updated = new QuestionUpsertRequestDTO(
                topicId,
                "MULTI_CHOICE",
                "After",
                "MARKDOWN",
                "Updated explanation",
                "HARD",
                2.5,
                45,
                "PUBLIC",
                List.of(
                        option("A", true),
                        option("C", true),
                        option("D", false)
                )
        );

        var result = service.update(subjectId, created.id(), ownerId, updated);

        assertThat(result.content()).isEqualTo("After");
        assertThat(result.options()).extracting(value -> value.optionKey().name())
                .containsExactly("A", "C", "D");
        assertThat(optionRepo.findByQuestionIdOrderByOptionKeyAsc(created.id())).hasSize(3);
    }

    @Test
    void onlyOwnerCanMutatePublicQuestion() {
        var created = service.create(subjectId, ownerId, request("PUBLIC", "Shared"));

        assertThatThrownBy(() -> service.archive(subjectId, created.id(), otherTeacherId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("QUESTION_FORBIDDEN");
    }

    @Test
    void archiveAndRestoreKeepQuestionAndOptions() {
        var created = service.create(subjectId, ownerId, request("PRIVATE", "Lifecycle"));
        int optionCount = optionRepo.findByQuestionIdOrderByOptionKeyAsc(created.id()).size();
        var collection = collectionService.create(
                subjectId,
                ownerId,
                new CollectionRequestDTO("Lifecycle-" + UUID.randomUUID(), null, "PRIVATE")
        );
        collectionItemService.add(
                subjectId, collection.id(), ownerId, List.of(created.id())
        );

        assertThat(service.archive(subjectId, created.id(), ownerId).status())
                .isEqualTo(QuestionStatus.ARCHIVED);
        assertThatThrownBy(() -> service.archive(subjectId, created.id(), ownerId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("QUESTION_ALREADY_ARCHIVED");
        assertThat(service.restore(subjectId, created.id(), ownerId).status())
                .isEqualTo(QuestionStatus.ACTIVE);
        assertThat(questionRepo.findById(created.id())).isPresent();
        assertThat(optionRepo.findByQuestionIdOrderByOptionKeyAsc(created.id()))
                .hasSize(optionCount);
        assertThat(collectionItemRepo.findQuestionIds(collection.id()))
                .containsExactly(created.id());
    }

    private QuestionUpsertRequestDTO request(String visibility, String content) {
        return new QuestionUpsertRequestDTO(
                topicId,
                "SINGLE_CHOICE",
                content,
                "PLAIN_TEXT",
                "Owner explanation",
                "EASY",
                1.0,
                30,
                visibility,
                List.of(option("A", true), option("B", false))
        );
    }

    private QuestionOptionRequestDTO option(String key, boolean correct) {
        return new QuestionOptionRequestDTO(
                key,
                "Option " + key,
                "PLAIN_TEXT",
                "Option explanation",
                correct
        );
    }

    private void assign(UUID teacherId) {
        SubjectTeacher assignment = new SubjectTeacher();
        assignment.setSubjectId(subjectId);
        assignment.setTeacherId(teacherId);
        assignment.setStatus(SubjectTeacherStatus.ACTIVE);
        subjectTeacherRepo.save(assignment);
    }
}
