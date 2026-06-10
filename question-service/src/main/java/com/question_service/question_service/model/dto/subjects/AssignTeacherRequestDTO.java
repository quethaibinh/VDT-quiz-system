package com.question_service.question_service.model.dto.subjects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssignTeacherRequestDTO {

    private UUID teacherId;

}
