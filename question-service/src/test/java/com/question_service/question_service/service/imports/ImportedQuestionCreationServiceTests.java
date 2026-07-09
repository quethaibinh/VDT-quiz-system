package com.question_service.question_service.service.imports;

import com.question_service.question_service.model.dto.imports.NormalizedImportQuestionRow;
import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.Difficulty;
import com.question_service.question_service.model.entity.ImportJob;
import com.question_service.question_service.model.entity.Question;
import com.question_service.question_service.model.entity.enums.QuestionStatus;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import com.question_service.question_service.model.entity.Topic;
import com.question_service.question_service.repository.ImportJobRepo;
import com.question_service.question_service.repository.QuestionOptionRepo;
import com.question_service.question_service.repository.QuestionRepo;
import com.question_service.question_service.repository.TopicRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ImportedQuestionCreationServiceTests {

    private final ImportJobRepo importJobRepo = mock(ImportJobRepo.class);
    private final TopicRepo topicRepo = mock(TopicRepo.class);
    private final QuestionRepo questionRepo = mock(QuestionRepo.class);
    private final QuestionOptionRepo questionOptionRepo = mock(QuestionOptionRepo.class);
    private final ImportedQuestionCreationService service = new ImportedQuestionCreationService(
            importJobRepo,
            topicRepo,
            questionRepo,
            questionOptionRepo
    );

    @Test
    void createsImportedQuestionWithActiveStatus() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "questions.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1}
        );
        NormalizedImportQuestionRow row = new NormalizedImportQuestionRow(
                2,
                "Philosophy",
                "SINGLE_CHOICE",
                "What is knowledge?",
                List.of(),
                new LinkedHashSet<>(),
                Difficulty.MEDIUM,
                1.0,
                0,
                null,
                QuestionVisibility.PUBLIC,
                ContentFormat.PLAIN_TEXT
        );

        when(importJobRepo.save(any(ImportJob.class))).thenAnswer(invocation -> {
            ImportJob importJob = invocation.getArgument(0);
            if (importJob.getId() == null) {
                importJob.setId(UUID.randomUUID());
            }
            return importJob;
        });
        when(topicRepo.findBySubjectIdAndNameIgnoreCase(subjectId, "Philosophy"))
                .thenReturn(Optional.empty());
        when(topicRepo.save(any(Topic.class))).thenAnswer(invocation -> {
            Topic topic = invocation.getArgument(0);
            topic.setId(UUID.randomUUID());
            return topic;
        });
        when(questionRepo.save(any(Question.class))).thenAnswer(invocation -> {
            Question question = invocation.getArgument(0);
            question.setId(UUID.randomUUID());
            return question;
        });

        service.createQuestions(subjectId, teacherId, file, List.of(row));

        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);
        org.mockito.Mockito.verify(questionRepo).save(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getVisibility()).isEqualTo(QuestionVisibility.PUBLIC);
        assertThat(questionCaptor.getValue().getStatus()).isEqualTo(QuestionStatus.ACTIVE);
    }
}
