package com.examruntime_service.examruntime_service.service.paper;

import com.examruntime_service.examruntime_service.client.QuestionMediaClient;
import com.examruntime_service.examruntime_service.model.dto.cache.ExamPaperPoolDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperOptionDTO;
import com.examruntime_service.examruntime_service.model.dto.cache.PaperQuestionDTO;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StudentPaperMapperTest {

    @Test
    void mapsQuestionWithoutImageWhenSignedUrlMapIsImmutableEmpty() {
        QuestionMediaClient mediaClient = mock(QuestionMediaClient.class);
        when(mediaClient.signedUrls(any())).thenReturn(Map.of());
        StudentPaperMapper mapper = new StudentPaperMapper(new ObjectMapper(), mediaClient);

        UUID examId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        ExamPaperPoolDTO pool = new ExamPaperPoolDTO(
                examId,
                1,
                1,
                0,
                0,
                List.of(new PaperQuestionDTO(
                        questionId,
                        1,
                        "EASY",
                        "SINGLE_CHOICE",
                        "Question content",
                        "PLAIN_TEXT",
                        1.0,
                        60,
                        null,
                        List.of(new PaperOptionDTO(optionId, "A", "Option A", "PLAIN_TEXT"))
                ))
        );

        var questions = mapper.mapToStudentQuestions(
                pool,
                List.of(questionId),
                Map.of(questionId, List.of(optionId))
        );

        assertThat(questions).hasSize(1);
        assertThat(questions.getFirst().getImageObjectKey()).isNull();
        assertThat(questions.getFirst().getImageUrl()).isNull();
        assertThat(questions.getFirst().getOptions()).hasSize(1);
    }
}
