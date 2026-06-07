package com.auth_service.auth_service.repository;

import com.auth_service.auth_service.model.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, UUID> {

    UserEntity findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByStudentCode(String studentCode);
    boolean existsByTeacherCode(String teacherCode);

}
