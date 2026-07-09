package com.examruntime_service.examruntime_service.client;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResultServiceResultClientTest {

    private final ResultServiceResultClient client = new ResultServiceResultClient(
            RestClient.builder(),
            new ObjectMapper(),
            "http://result-service:8085",
            "test-key"
    );

    @Test
    void parsesWrappedResultServiceStatusResponse() throws Exception {
        UUID submissionId = UUID.randomUUID();

        Set<UUID> gradedIds = client.parseGradedSubmissionIds("""
                {
                  "status": 200,
                  "message": "Success",
                  "data": {
                    "gradedSubmissionIds": ["%s"]
                  }
                }
                """.formatted(submissionId));

        assertThat(gradedIds).containsExactly(submissionId);
    }

    @Test
    void parsesRawResultServiceStatusResponse() throws Exception {
        UUID submissionId = UUID.randomUUID();

        Set<UUID> gradedIds = client.parseGradedSubmissionIds("""
                {"gradedSubmissionIds":["%s"]}
                """.formatted(submissionId));

        assertThat(gradedIds).containsExactly(submissionId);
    }
}
