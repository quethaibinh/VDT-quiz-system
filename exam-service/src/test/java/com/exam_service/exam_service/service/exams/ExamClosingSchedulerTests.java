package com.exam_service.exam_service.service.exams;

import com.exam_service.exam_service.repository.ExamRepo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
@Transactional
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestPropertySource(properties = {
        "exam.closing.enabled=true",
        "exam.closing.batch-size=10",
        "exam.closing.grace-period-seconds=60"
})
class ExamClosingSchedulerTests {

    @Autowired
    private ExamClosingScheduler scheduler;

    @MockitoBean
    private ExamRepo examRepo;

    @MockitoBean
    private ExamClosingTransactionService closingTransactionService;

    @MockitoBean
    private KafkaTemplate<?, ?> kafkaTemplate;

    @Test
    void schedulerProcessesEndedExamsAndContinuesOnError() {
        UUID examId1 = UUID.randomUUID();
        UUID examId2 = UUID.randomUUID();

        when(examRepo.findCandidateIdsForClosing(any(OffsetDateTime.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(examId1, examId2)));

        when(closingTransactionService.closeIfEnded(examId1, 60)).thenReturn(true);
        when(closingTransactionService.closeIfEnded(examId2, 60)).thenThrow(new RuntimeException("Simulated failure"));

        scheduler.closeEndedExams();

        verify(closingTransactionService).closeIfEnded(examId1, 60);
        verify(closingTransactionService).closeIfEnded(examId2, 60);
    }
}
