package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.client.QuestionCollectionMetadata;
import com.exam_service.exam_service.client.QuestionServiceClient;
import com.exam_service.exam_service.model.dto.exams.ExamDraftRequestDTO;
import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.AssignmentStatus;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.model.entity.enums.ExamType;
import com.exam_service.exam_service.repository.ExamAssignmentRepo;
import com.exam_service.exam_service.repository.ExamRepo;
import com.exam_service.exam_service.util.exception.ConflictException;
import com.exam_service.exam_service.util.exception.NotFoundException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TeacherExamDraftServiceTests {

    private final ExamRepo examRepo = mock(ExamRepo.class);
    private final ExamAssignmentRepo assignmentRepo = mock(ExamAssignmentRepo.class);
    private final QuestionServiceClient questionClient = mock(QuestionServiceClient.class);
    private TeacherExamDraftService service;

    @BeforeEach
    void setUp() {
        service = new TeacherExamDraftService(examRepo, assignmentRepo, questionClient);
        when(examRepo.existsByCode(any())).thenReturn(false);
        when(examRepo.save(any())).thenAnswer(invocation -> {
            Exam exam = invocation.getArgument(0);
            if (exam.getId() == null) {
                exam.setId(UUID.randomUUID());
            }
            return exam;
        });
    }

    @Test
    void createsDraftFromAuthoritativeCollectionMetadata() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        when(questionClient.getExamMetadata(subjectId, collectionId, teacherId))
                .thenReturn(new QuestionCollectionMetadata(
                        collectionId, subjectId, "Mathematics", "Pool", 10, 10, 10
                ));
        when(assignmentRepo.countByExamIdAndStatus(any(), eq(AssignmentStatus.ASSIGNED)))
                .thenReturn(0L);

        var result = service.create(subjectId, teacherId, request(collectionId, 3, 4, 5));

        assertThat(result.status()).isEqualTo(ExamStatus.DRAFT);
        assertThat(result.collectionName()).isEqualTo("Pool");
        assertThat(result.easyCount()).isEqualTo(3);
        assertThat(result.endAt()).isEqualTo(result.startAt().plusMinutes(60));
        verify(examRepo).save(any(Exam.class));
    }

    @Test
    void rejectsQuotaLargerThanCollectionCapacityWithoutSaving() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        when(questionClient.getExamMetadata(subjectId, collectionId, teacherId))
                .thenReturn(new QuestionCollectionMetadata(
                        collectionId, subjectId, "Mathematics", "Pool", 1, 1, 1
                ));

        assertThatThrownBy(() -> service.create(
                subjectId, teacherId, request(collectionId, 2, 0, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INSUFFICIENT_COLLECTION_QUOTA");

        verify(examRepo, never()).save(any());
    }

    @Test
    void hidesExamOwnedByAnotherTeacher() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        when(examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(examId, subjectId, teacherId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(subjectId, examId, teacherId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("EXAM_NOT_FOUND");
    }

    @Test
    void updatesOwnedDraftAndRefreshesAuthoritativeCollectionSnapshot() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        Exam exam = draft(examId, subjectId, teacherId);
        when(examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(examId, subjectId, teacherId))
                .thenReturn(Optional.of(exam));
        when(questionClient.getExamMetadata(subjectId, collectionId, teacherId))
                .thenReturn(new QuestionCollectionMetadata(
                        collectionId, subjectId, "Mathematics", "Updated Pool", 5, 6, 7
                ));
        when(assignmentRepo.countByExamIdAndStatus(examId, AssignmentStatus.ASSIGNED))
                .thenReturn(2L);

        var result = service.update(
                subjectId,
                examId,
                teacherId,
                request(collectionId, 2, 3, 4)
        );

        assertThat(result.collectionName()).isEqualTo("Updated Pool");
        assertThat(result.easyCount()).isEqualTo(2);
        assertThat(result.mediumCount()).isEqualTo(3);
        assertThat(result.hardCount()).isEqualTo(4);
        assertThat(result.assignedCount()).isEqualTo(2);
        assertThat(exam.getCreatedByTeacherId()).isEqualTo(teacherId);
        verify(examRepo).save(exam);
    }

    @Test
    void cancelsOwnedDraft() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Exam exam = draft(examId, subjectId, teacherId);
        when(examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(examId, subjectId, teacherId))
                .thenReturn(Optional.of(exam));

        var result = service.cancel(subjectId, examId, teacherId);

        assertThat(result.status()).isEqualTo(ExamStatus.CANCELLED);
        verify(examRepo).save(exam);
    }

    @Test
    void rejectsMutationWhenExamIsNotDraft() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Exam cancelled = draft(examId, subjectId, teacherId);
        cancelled.setStatus(ExamStatus.CANCELLED);
        when(examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(examId, subjectId, teacherId))
                .thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.cancel(subjectId, examId, teacherId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("EXAM_NOT_EDITABLE");

        verify(examRepo, never()).save(any());
    }

    @Test
    void hidesForeignExamFromMutation() {
        UUID subjectId = UUID.randomUUID();
        UUID examId = UUID.randomUUID();
        UUID foreignTeacherId = UUID.randomUUID();
        when(examRepo.findByIdAndSubjectIdAndCreatedByTeacherId(
                examId, subjectId, foreignTeacherId
        )).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancel(subjectId, examId, foreignTeacherId))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("EXAM_NOT_FOUND");

        verify(examRepo, never()).save(any());
    }

    @Test
    void rejectsZeroQuotaBeforeCallingQuestionService() {
        UUID collectionId = UUID.randomUUID();

        assertThatThrownBy(() -> service.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                request(collectionId, 0, 0, 0)
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("QUESTION_QUOTA_REQUIRED");

        verifyNoInteractions(questionClient);
        verify(examRepo, never()).save(any());
    }

    @Test
    void rejectsInvalidPolicyEnumsWithoutSaving() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        UUID collectionId = UUID.randomUUID();
        when(questionClient.getExamMetadata(subjectId, collectionId, teacherId))
                .thenReturn(new QuestionCollectionMetadata(
                        collectionId, subjectId, "Mathematics", "Pool", 10, 10, 10
                ));
        ExamDraftRequestDTO invalidPolicy = request(
                collectionId, 1, 1, 1, "SOMEDAY", "LOCK"
        );

        assertThatThrownBy(() -> service.create(subjectId, teacherId, invalidPolicy))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("INVALID_SHOW_RESULT_POLICY");

        verify(examRepo, never()).save(any());
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Test
    void listsOnlyOwnedSubjectDraftsWithKeywordAndRequestedPage() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();
        Exam exam = draft(UUID.randomUUID(), subjectId, teacherId);
        exam.setTitle("Calculus Midterm");
        when(examRepo.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(exam)));
        when(assignmentRepo.countByExamIdAndStatus(exam.getId(), AssignmentStatus.ASSIGNED))
                .thenReturn(3L);

        var result = service.list(
                subjectId, teacherId, "draft", " calculus ", 2, 15, "title,asc"
        );

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().getFirst().assignedCount()).isEqualTo(3);

        ArgumentCaptor<Specification<Exam>> specificationCaptor =
                ArgumentCaptor.forClass(Specification.class);
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(examRepo).findAll(specificationCaptor.capture(), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(15);
        assertThat(pageableCaptor.getValue().getSort().getOrderFor("title").isAscending())
                .isTrue();

        Root<Exam> root = mock(Root.class);
        CriteriaQuery<?> query = mock(CriteriaQuery.class);
        Predicate predicate = mock(Predicate.class);
        CriteriaBuilder cb = mock(CriteriaBuilder.class, invocation ->
                invocation.getMethod().getReturnType().equals(Predicate.class)
                        ? predicate
                        : RETURNS_DEFAULTS.answer(invocation)
        );
        Path subjectPath = mock(Path.class);
        Path ownerPath = mock(Path.class);
        Path examTypePath = mock(Path.class);
        Path statusPath = mock(Path.class);
        Path titlePath = mock(Path.class);
        Path codePath = mock(Path.class);
        when(root.get("subjectId")).thenReturn(subjectPath);
        when(root.get("createdByTeacherId")).thenReturn(ownerPath);
        when(root.get("examType")).thenReturn(examTypePath);
        when(root.get("status")).thenReturn(statusPath);
        when(root.get("title")).thenReturn(titlePath);
        when(root.get("code")).thenReturn(codePath);
        when(cb.lower(any())).thenReturn(mock(jakarta.persistence.criteria.Expression.class));

        specificationCaptor.getValue().toPredicate(root, query, cb);

        verify(cb).equal(subjectPath, subjectId);
        verify(cb).equal(ownerPath, teacherId);
        verify(cb).equal(examTypePath, ExamType.STANDARD_EXAM);
        verify(cb).equal(statusPath, ExamStatus.DRAFT);
        verify(cb, times(2)).like(any(), eq("%calculus%"));
    }

    @Test
    void rejectsInvalidListStatusAndPagination() {
        UUID subjectId = UUID.randomUUID();
        UUID teacherId = UUID.randomUUID();

        assertThatThrownBy(() -> service.list(
                subjectId, teacherId, "unknown", null, 0, 20, null
        )).hasMessage("INVALID_EXAM_STATUS");
        assertThatThrownBy(() -> service.list(
                subjectId, teacherId, null, null, -1, 20, null
        )).hasMessage("INVALID_PAGE_REQUEST");
        assertThatThrownBy(() -> service.list(
                subjectId, teacherId, null, null, 0, 20, "status,asc"
        )).hasMessage("INVALID_SORT_FIELD");

        verify(examRepo, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    private Exam draft(UUID examId, UUID subjectId, UUID teacherId) {
        Exam exam = new Exam();
        exam.setId(examId);
        exam.setCode("EXAM000001");
        exam.setTitle("Midterm");
        exam.setSubjectId(subjectId);
        exam.setSubjectNameSnapshot("Mathematics");
        exam.setCreatedByTeacherId(teacherId);
        exam.setStatus(ExamStatus.DRAFT);
        exam.setCollectionId(UUID.randomUUID());
        exam.setCollectionNameSnapshot("Pool");
        exam.setEasyCount(1);
        exam.setMediumCount(1);
        exam.setHardCount(1);
        exam.setStartAt(OffsetDateTime.parse("2026-07-01T08:00:00+07:00"));
        exam.setEndAt(exam.getStartAt().plusMinutes(60));
        exam.setDurationMinutes(60);
        return exam;
    }

    private ExamDraftRequestDTO request(
            UUID collectionId,
            int easy,
            int medium,
            int hard
    ) {
        return request(collectionId, easy, medium, hard, "AFTER_CLOSED", "LOCK");
    }

    private ExamDraftRequestDTO request(
            UUID collectionId,
            int easy,
            int medium,
            int hard,
            String showResultPolicy,
            String handleViolation
    ) {
        return new ExamDraftRequestDTO(
                "Midterm",
                "Description",
                collectionId,
                easy,
                medium,
                hard,
                OffsetDateTime.parse("2026-07-01T08:00:00+07:00"),
                60,
                10,
                0,
                true,
                true,
                showResultPolicy,
                true,
                true,
                5,
                handleViolation
        );
    }
}
