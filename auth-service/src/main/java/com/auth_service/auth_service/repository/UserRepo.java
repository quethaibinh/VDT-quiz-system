package com.auth_service.auth_service.repository;

import com.auth_service.auth_service.model.entity.UserEntity;
import com.auth_service.auth_service.model.entity.UserType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEntity, UUID> {

    UserEntity findByUsername(String username);

    boolean existsByUsername(String username);
    boolean existsByStudentCode(String studentCode);
    boolean existsByTeacherCode(String teacherCode);

    @Query("""
            select user
            from UserEntity user
            where user.userType = :userType
              and user.status = :status
              and (
                    :keyword = ''
                    or lower(coalesce(user.studentCode, '')) like concat('%', :keyword, '%')
                    or lower(user.fullName) like concat('%', :keyword, '%')
                    or lower(coalesce(user.displayName, '')) like concat('%', :keyword, '%')
              )
            """)
    Page<UserEntity> searchStudents(
            @Param("userType") UserType userType,
            @Param("status") String status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    List<UserEntity> findAllByIdIn(Collection<UUID> ids);
}
