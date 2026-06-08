package com.question_service.question_service.service.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportedQuestionCreationResult {

    private UUID importJobId;
    private int createdTopicCount;
    private int createdQuestionCount;

}
