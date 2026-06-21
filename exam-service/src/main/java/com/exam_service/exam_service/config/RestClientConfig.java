package com.exam_service.exam_service.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
/**
 * Cau hinh timeout chung cho cac loi goi dong bo tu Exam Service sang service khac.
 */
public class RestClientConfig {

    @Bean
    /**
     * Gioi han thoi gian cho de request giao vien khong bi treo khi service dich loi.
     */
    public RestClient.Builder restClientBuilder() {
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(client);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory);
    }
}
