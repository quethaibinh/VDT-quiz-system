package com.exam_service.exam_service.service.livequiz;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionSnapshotItem;
import com.exam_service.exam_service.client.QuestionSnapshotOption;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveQuizPrepareTransactionServiceTests {

    private final ExamRepo examRepo = mock(ExamRepo.class);
    private final ExamQuestionRepo questionRepo = mock(ExamQuestionRepo.class);
    private final LiveQuizPrepareTransactionService service =
            new LiveQuizPrepareTransactionService(examRepo, questionRepo, JsonMapper.builder().build());

    @Test
    void prepareFreezesAllQuestionsAndMarksPrepared() {
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        UUID correctOptionId = UUID.randomUUID();
        Exam quiz = liveQuiz(subjectId, teacherId, collectionId);
        quiz.setId(quizId);
        when(examRepo.findOwnedForUpdate(quizId, subjectId, teacherId)).thenReturn(Optional.of(quiz));

        QuestionCollectionSnapshot snapshot = new QuestionCollectionSnapshot(
                collectionId,
                subjectId,
                "Math",
                "Quick Pool",
                List.of(item(correctOptionId, "EASY"), item(UUID.randomUUID(), "MEDIUM"))
        );

        service.prepare(subjectId, quizId, teacherId, 0, snapshot);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ExamQuestion>> rows =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(questionRepo).saveAll(rows.capture());
        assertThat(rows.getValue()).hasSize(2);
        assertThat(rows.getValue().getFirst().getQuestionSnapshot()).doesNotContain("correct");
        assertThat(rows.getValue().getFirst().getAnswerKeySnapshot()).contains(correctOptionId.toString());
        assertThat(rows.getValue().getFirst().getTimeLimitSeconds()).isEqualTo(30);
        assertThat(quiz.getStatus()).isEqualTo(ExamStatus.PREPARED);
        assertThat(quiz.getSnapshotVersion()).isEqualTo(1);
    }

    @Test
    void prepareRejectsStandardExam() {
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        Exam exam = liveQuiz(subjectId, teacherId, collectionId);
        exam.setExamType(ExamType.STANDARD_EXAM);
        when(examRepo.findOwnedForUpdate(quizId, subjectId, teacherId)).thenReturn(Optional.of(exam));

        QuestionCollectionSnapshot snapshot = new QuestionCollectionSnapshot(
                collectionId,
                subjectId,
                "Math",
                "Pool",
                List.of(item(UUID.randomUUID(), "EASY"))
        );

        assertThatThrownBy(() -> service.prepare(subjectId, quizId, teacherId, 0, snapshot))
                .isInstanceOf(com.exam_service.exam_service.util.exception.NotFoundException.class)
                .hasMessage("LIVE_QUIZ_NOT_FOUND");
    }

    @Test
    void prepareRejectsQuestionWithoutTimeLimit() {
        UUID subjectId = UUID.randomUUID();
        UUID quizId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        Exam quiz = liveQuiz(subjectId, teacherId, collectionId);
        when(examRepo.findOwnedForUpdate(quizId, subjectId, teacherId)).thenReturn(Optional.of(quiz));

        QuestionSnapshotItem invalid = new QuestionSnapshotItem(
                UUID.randomUUID(),
                1,
                "EASY",
                "SINGLE_CHOICE",
                "Q?",
                "PLAIN_TEXT",
                1.0,
                0,
                List.of(
                        new QuestionSnapshotOption(UUID.randomUUID(), "A", "A", "PLAIN_TEXT", true),
                        new QuestionSnapshotOption(UUID.randomUUID(), "B", "B", "PLAIN_TEXT", false)
                )
        );
        QuestionCollectionSnapshot snapshot = new QuestionCollectionSnapshot(
                collectionId,
                subjectId,
                "Math",
                "Pool",
                List.of(invalid)
        );

        assertThatThrownBy(() -> service.prepare(subjectId, quizId, teacherId, 0, snapshot))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_EXAM_QUESTION_TIME_LIMIT");
    }

    private Exam liveQuiz(UUID subjectId, UUID teacherId, UUID collectionId) {
        Exam quiz = new Exam();
        quiz.setSubjectId(subjectId);
        quiz.setCreatedByTeacherId(teacherId);
        quiz.setCollectionId(collectionId);
        quiz.setStatus(ExamStatus.DRAFT);
        quiz.setExamType(ExamType.LIVE_QUIZ);
        return quiz;
    }

    private QuestionSnapshotItem item(UUID correctOptionId, String difficulty) {
        return new QuestionSnapshotItem(
                UUID.randomUUID(),
                1,
                difficulty,
                "SINGLE_CHOICE",
                "Q?",
                "PLAIN_TEXT",
                1.0,
                30,
                List.of(
                        new QuestionSnapshotOption(correctOptionId, "A", "A", "PLAIN_TEXT", true),
                        new QuestionSnapshotOption(UUID.randomUUID(), "B", "B", "PLAIN_TEXT", false)
                )
        );
    }
}
