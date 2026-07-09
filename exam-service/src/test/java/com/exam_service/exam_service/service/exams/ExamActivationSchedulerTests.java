package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.model.entity.Exam;
import com.exam_service.exam_service.model.entity.enums.ExamStatus;
import com.exam_service.exam_service.repository.ExamRepo;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "exam.activation.enabled=true",
        "exam.activation.batch-size=10"
})
class ExamActivationSchedulerTests {

    @Autowired
    private ExamActivationScheduler scheduler;

    @MockitoBean
    private ExamRepo examRepo;

    @MockitoBean
    private ExamActivationTransactionService activationTransactionService;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Autowired
    private Clock clock;

    @Test
    void schedulerProcessesDueExamsAndContinuesOnError() {
        UUID examId1 = UUID.randomUUID();
        UUID examId2 = UUID.randomUUID();

        when(examRepo.findCandidateIdsForActivation(eq(ExamStatus.SCHEDULED), any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(examId1, examId2)));

        when(activationTransactionService.activateIfDue(examId1)).thenReturn(true);
        when(activationTransactionService.activateIfDue(examId2)).thenThrow(new RuntimeException("Simulated failure"));

        scheduler.activateExams();

        verify(activationTransactionService).activateIfDue(examId1);
        verify(activationTransactionService).activateIfDue(examId2);
    }
}
