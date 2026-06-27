package com.examruntime_service.examruntime_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ExamruntimeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(ExamruntimeServiceApplication.class, args);
	}

}
