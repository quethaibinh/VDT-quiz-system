package com.examruntime_service.examruntime_service.service.session;

import com.examruntime_service.examruntime_service.client.ExamServiceAssignmentClient;
import com.examruntime_service.examruntime_service.model.dto.cache.InternalExamAssignmentDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RuntimeAssignmentResolverTest {

    private StringRedisTemplate redisTemplate;
    private SetOperations<String, String> setOps;
    private HashOperations<String, Object, Object> hashOps;
    private ValueOperations<String, String> valueOps;
    private ExamServiceAssignmentClient client;
    private RuntimeAssignmentResolver resolver;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        setOps = mock(SetOperations.class);
        hashOps = mock(HashOperations.class);
        valueOps = mock(ValueOperations.class);

        when(redisTemplate.opsForSet()).thenReturn(setOps);
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);

        client = mock(ExamServiceAssignmentClient.class);
        resolver = new RuntimeAssignmentResolver(redisTemplate, client, new ObjectMapper());
    }

    @Test
    void testCacheHitReturnsAssignmentId() {
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        String setKey = "exam:" + examId + ":students";
        String hashKey = "exam:" + examId + ":student-assignments";

        when(redisTemplate.hasKey(setKey)).thenReturn(true);
        when(setOps.isMember(setKey, studentId.toString())).thenReturn(true);
        when(hashOps.get(hashKey, studentId.toString())).thenReturn(assignmentId.toString());

        UUID result = resolver.resolveAssignmentId(examId, studentId);

        assertThat(result).isEqualTo(assignmentId);
        verify(client, never()).getAssignments(any());
    }

    @Test
    void testCacheMissHydratesFromFallback() {
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        String setKey = "exam:" + examId + ":students";
        String hashKey = "exam:" + examId + ":student-assignments";

        // Khong co key trong cache
        when(redisTemplate.hasKey(setKey)).thenReturn(false);

        // Mock API fallback
        InternalExamAssignmentDTO.AssignmentDetail detail = new InternalExamAssignmentDTO.AssignmentDetail(assignmentId, studentId);
        InternalExamAssignmentDTO fallbackDto = new InternalExamAssignmentDTO(examId, List.of(detail));
        when(client.getAssignments(examId)).thenReturn(fallbackDto);

        UUID result = resolver.resolveAssignmentId(examId, studentId);

        assertThat(result).isEqualTo(assignmentId);
        verify(redisTemplate).delete(setKey);
        verify(redisTemplate).delete(hashKey);
        verify(setOps).add(eq(setKey), any(String[].class));
        verify(hashOps).putAll(eq(hashKey), any());
    }

    @Test
    void testPartialCacheFallsBackAndHydratesAgain() {
        UUID examId = UUID.randomUUID();
        UUID studentId = UUID.randomUUID();
        UUID assignmentId = UUID.randomUUID();

        String setKey = "exam:" + examId + ":students";
        String hashKey = "exam:" + examId + ":student-assignments";

        when(redisTemplate.hasKey(setKey)).thenReturn(true);
        when(setOps.isMember(setKey, studentId.toString())).thenReturn(true);
        when(hashOps.get(hashKey, studentId.toString())).thenReturn(null);

        InternalExamAssignmentDTO.AssignmentDetail detail = new InternalExamAssignmentDTO.AssignmentDetail(assignmentId, studentId);
        InternalExamAssignmentDTO fallbackDto = new InternalExamAssignmentDTO(examId, List.of(detail));
        when(client.getAssignments(examId)).thenReturn(fallbackDto);

        UUID result = resolver.resolveAssignmentId(examId, studentId);

        assertThat(result).isEqualTo(assignmentId);
        verify(client).getAssignments(examId);
        verify(redisTemplate).delete(setKey);
        verify(redisTemplate).delete(hashKey);
    }
}
