package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.client.QuestionCollectionSnapshot;
import com.exam_service.exam_service.client.QuestionSnapshotItem;
import com.exam_service.exam_service.client.QuestionSnapshotOption;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.ExamQuestion;
import com.exam_service.exam_service.model.entity.OutboxEvent;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.repository.ExamQuestionRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.repository.OutboxEventRepo;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSchedulingTransactionServiceTests {

    private final ExamRepo examRepo = mock(ExamRepo.class);
    private final ExamQuestionRepo questionRepo = mock(ExamQuestionRepo.class);
    private final ExamAssignmentRepo assignmentRepo = mock(ExamAssignmentRepo.class);
    private final OutboxEventRepo outboxRepo = mock(OutboxEventRepo.class);
    private final ExamSchedulingTransactionService service =
            new ExamSchedulingTransactionService(
                    examRepo,
                    questionRepo,
                    assignmentRepo,
                    outboxRepo,
                    JsonMapper.builder().build()
            );

    @Test
    void schedulesSnapshotAndSeparatesPaperFromAnswerKey() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        UUID correctOptionId = UUID.randomUUID();
        Exam exam = draft(subjectId, teacherId, collectionId);
        exam.setId(examId);
        exam.setEasyCount(1);
        when(examRepo.findOwnedForUpdate(examId, subjectId, teacherId))
                .thenReturn(Optional.of(exam));
        when(assignmentRepo.countByExamIdAndStatus(any(), any())).thenReturn(1L);

        QuestionCollectionSnapshot snapshot = new QuestionCollectionSnapshot(
                collectionId,
                subjectId,
                "Math",
                "Pool",
                List.of(new QuestionSnapshotItem(
                        questionId,
                        2,
                        "EASY",
                        "SINGLE_CHOICE",
                        "1 + 1 = ?",
                        "PLAIN_TEXT",
                        1.0,
                        List.of(
                                new QuestionSnapshotOption(
                                        correctOptionId, "A", "2", "PLAIN_TEXT", true
                                ),
                                new QuestionSnapshotOption(
                                        UUID.randomUUID(), "B", "3", "PLAIN_TEXT", false
                                )
                        )
                ))
        );

        service.schedule(subjectId, examId, teacherId, 0, snapshot);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<ExamQuestion>> rows =
                org.mockito.ArgumentCaptor.forClass(List.class);
        verify(questionRepo).saveAll(rows.capture());
        ExamQuestion row = rows.getValue().getFirst();
        assertThat(row.getQuestionSnapshot()).doesNotContain("correct");
        assertThat(row.getAnswerKeySnapshot()).contains(correctOptionId.toString());
        assertThat(exam.getStatus()).isEqualTo(ExamStatus.SCHEDULED);
        assertThat(exam.getSnapshotVersion()).isEqualTo(1);

        org.mockito.ArgumentCaptor<OutboxEvent> event =
                org.mockito.ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepo).save(event.capture());
        assertThat(event.getValue().getPayload()).contains(examId.toString());
    }

    private Exam draft(UUID subjectId, UUID teacherId, UUID collectionId) {
        Exam exam = new Exam();
        exam.setSubjectId(subjectId);
        exam.setCreatedByTeacherId(teacherId);
        exam.setCollectionId(collectionId);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setStartAt(OffsetDateTime.now().plusHours(2));
        exam.setEndAt(OffsetDateTime.now().plusHours(3));
        return exam;
    }
}
