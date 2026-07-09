package com.question_service.question_service.model.dto.imports;

import com.question_service.question_service.model.entity.enums.ContentFormat;
import com.question_service.question_service.model.entity.enums.Difficulty;
import com.question_service.question_service.model.entity.enums.OptionKey;
import com.question_service.question_service.model.entity.enums.QuestionVisibility;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalizedImportQuestionRow {

    private int rowNumber;
    private String topicName;
    private String questionType;
    private String content;
    private List<NormalizedQuestionOptionDTO> options;
    private Set<OptionKey> correctOptionKeys;
    private Difficulty difficulty;
    private Double defaultScore;
    private int estimatedSecond;
    private String explanation;
    private QuestionVisibility visibility;
    private ContentFormat contentFormat;

}
