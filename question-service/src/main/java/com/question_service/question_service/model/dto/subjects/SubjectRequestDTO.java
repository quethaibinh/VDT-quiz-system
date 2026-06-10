package com.question_service.question_service.model.dto.subjects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectRequestDTO {

    private String code;
    private String name;
    private String description;

}
