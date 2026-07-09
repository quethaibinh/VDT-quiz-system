package com.question_service.question_service.model.dto.subjects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SubjectTeacherResponseDTO {

    private UUID id;
    private UUID subjectId;
    private UUID teacherId;
    private String status;
    private UUID assignedByAdminId;
    private LocalDateTime assignedAt;
    private String teacherCode;
    private String fullName;
    private String displayName;
    private String email;
    private String teacherStatus;
    private String identityState;

}
