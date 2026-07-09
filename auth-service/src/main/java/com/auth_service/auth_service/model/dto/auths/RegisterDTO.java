package com.auth_service.auth_service.model.dto.auths;

import com.auth_service.auth_service.model.entity.UserGender;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegisterDTO {

    private String username;
    private String password;
    private String fullName;
    private String email;
    private String studentCode;
    private String teacherCode;
    private String phone;
    private String nationalId;
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private UserGender gender;

}
