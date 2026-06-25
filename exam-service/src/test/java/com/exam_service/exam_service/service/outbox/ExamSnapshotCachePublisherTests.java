package com.exam_service.exam_service.service.outbox;

import com.exam_service.exam_service.model.dto.cache.ExamAnswerKeyDTO;
import com.exam_service.exam_service.model.dto.cache.ExamPaperPoolDTO;
import com.exam_service.exam_service.model.dto.outbox.ExamSnapshotCacheRequested;
import com.exam_service.exam_service.model.dto.assignments.InternalExamAssignmentDTO;
import com.exam_service.exam_service.service.exams.ExamAssignmentSnapshotService;
import com.exam_service.exam_service.service.exams.ExamSnapshotReadService;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.HashOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExamSnapshotCachePublisherTests {

    @Test
    @SuppressWarnings("unchecked")
    void writesBothVersionedKeysWithAtomicTtl() {
        UUID examId = UUID.randomUUID();
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        SetOperations setOps = mock(SetOperations.class);
        HashOperations hashOps = mock(HashOperations.class);
        
        ExamSnapshotReadService readService = mock(ExamSnapshotReadService.class);
        ExamAssignmentSnapshotService assignmentService = mock(ExamAssignmentSnapshotService.class);
        
        when(redis.opsForValue()).thenReturn(values);
        when(redis.opsForSet()).thenReturn(setOps);
        when(redis.opsForHash()).thenReturn(hashOps);
        
        when(readService.getPaperPool(examId))
                .thenReturn(new ExamPaperPoolDTO(examId, 1, 1, 0, 0, List.of()));
        when(readService.getAnswerKey(examId))
                .thenReturn(new ExamAnswerKeyDTO(examId, 1, List.of()));
        when(assignmentService.getAssignmentsSnapshot(examId))
                .thenReturn(new InternalExamAssignmentDTO(examId, List.of()));

        ExamSnapshotCachePublisher publisher = new ExamSnapshotCachePublisher(
                redis, JsonMapper.builder().build(), readService, assignmentService
        );

        OffsetDateTime expiresAt = OffsetDateTime.now().plusHours(25);
        publisher.publish(new ExamSnapshotCacheRequested(examId, 1, expiresAt));

        org.mockito.ArgumentCaptor<Duration> ttl =
                org.mockito.ArgumentCaptor.forClass(Duration.class);
        verify(values, times(2)).set(anyString(), anyString(), ttl.capture());
        assertThat(ttl.getAllValues())
                .allSatisfy(value -> assertThat(value.toHours()).isBetween(24L, 25L));
    }
}
