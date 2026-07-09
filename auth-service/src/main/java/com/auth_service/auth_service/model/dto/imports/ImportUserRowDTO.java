package com.auth_service.auth_service.model.dto.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportUserRowDTO {

    private int rowNumber;
    private String username;
    private String password;
    private String fullName;
    private String email;
    private String studentCode;
    private String teacherCode;
    private String phone;
    private String nationalId;
    private String birthDate;
    private String gender;
}
