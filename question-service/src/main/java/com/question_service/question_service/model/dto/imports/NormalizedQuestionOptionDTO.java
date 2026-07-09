package com.question_service.question_service.model.dto.imports;

import com.question_service.question_service.model.entity.enums.OptionKey;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NormalizedQuestionOptionDTO {

    private OptionKey optionKey;
    private String content;

}
