package com.exam_service.exam_service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@TestPropertySource(properties = {
        "exam.outbox.enabled=false",
        "exam.activation.enabled=false"
})
class ExamServiceApplicationTests {

    @MockitoBean
    private org.springframework.kafka.core.KafkaTemplate<?, ?> kafkaTemplate;

	@Test
	void contextLoads() {
	}

}
