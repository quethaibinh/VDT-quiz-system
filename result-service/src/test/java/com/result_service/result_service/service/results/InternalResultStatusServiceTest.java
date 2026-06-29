package com.result_service.result_service.service.results;

import com.result_service.result_service.repository.ExamResultRepo;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InternalResultStatusServiceTest {

    @Test
    void returnsExistingSubmissionIdsFromRepository() {
        ExamResultRepo repo = mock(ExamResultRepo.class);
        UUID existing = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        when(repo.findExistingSubmissionIds(List.of(existing, missing))).thenReturn(List.of(existing));

        InternalResultStatusService service = new InternalResultStatusService(repo);

        assertThat(service.existingSubmissionIds(List.of(existing, missing))).containsExactly(existing);
        verify(repo).findExistingSubmissionIds(List.of(existing, missing));
    }

    @Test
    void emptyInputDoesNotQueryRepository() {
        ExamResultRepo repo = mock(ExamResultRepo.class);
        InternalResultStatusService service = new InternalResultStatusService(repo);

        assertThat(service.existingSubmissionIds(List.of())).isEmpty();
        verifyNoInteractions(repo);
    }
}
