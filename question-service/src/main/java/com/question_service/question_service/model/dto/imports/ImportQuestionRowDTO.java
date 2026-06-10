package com.question_service.question_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportQuestionRowDTO {

    private int rowNumber;
    private String topicName;
    private String questionType;
    private String content;
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private String optionE;
    private String optionF;
    private String optionG;
    private String correctOptions;
    private String difficulty;
    private String defaultScore;
    private String estimatedSecond;
    private String explanation;
    private String status;
    private String contentFormat;

}
