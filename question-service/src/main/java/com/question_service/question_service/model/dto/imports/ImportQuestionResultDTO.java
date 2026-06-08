package com.question_service.question_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportQuestionResultDTO {

    private int totalRows;
    private int successCount;
    private int failedCount;
    private boolean imported;
    private UUID importJobId;
    private int createdTopicCount;
    private int createdQuestionCount;
    private List<ImportQuestionErrorDTO> errors = new ArrayList<>();

    public void addError(ImportQuestionErrorDTO error) {
        errors.add(error);
        failedCount = errors.size();
    }

}
