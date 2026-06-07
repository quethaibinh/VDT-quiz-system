package com.auth_service.auth_service.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "user_permission")
public class Permission extends BaseEntity{

    @Column(name = "code", nullable = false, unique = true)
    @Enumerated(EnumType.STRING)
    private PermissionCode code;
    private String name;
    private String description;

}
