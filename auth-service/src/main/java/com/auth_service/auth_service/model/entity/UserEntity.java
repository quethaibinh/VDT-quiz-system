package com.auth_service.auth_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_type_status", columnList = "user_type, status"),
        @Index(name = "idx_student_code", columnList = "student_code"),
        @Index(name = "idx_teacher_code", columnList = "teacher_code"),
        @Index(name = "idx_cccd_blind", columnList = "national_id_hash"),
        @Index(name = "idx_phone_blind", columnList = "phone_hash")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserEntity extends BaseEntity {

    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    private String email;

    @Column(name = "full_name",  nullable = false)
    private String fullName;
    private String displayName;

    @Enumerated(EnumType.STRING)
    private UserType userType;
    private String status;

    @Column(name = "student_code", nullable = true, unique = true)
    private String studentCode;

    @Column(name = "teacher_code", nullable = true, unique = true)
    private String teacherCode;
    private String phoneHash;
    private String phoneEncrypt;

    @Column(name = "national_id_hash", unique = true)
    private String nationalIdHash;
    private String nationalIdEncrypt;
    private String avatarUrl;
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    private UserGender gender;



}
