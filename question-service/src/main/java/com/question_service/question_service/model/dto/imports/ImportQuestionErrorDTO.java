package com.question_service.question_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportQuestionErrorDTO {

    private int rowNumber;
    private String fieldName;
    private String errorCode;
    private String message;

}
