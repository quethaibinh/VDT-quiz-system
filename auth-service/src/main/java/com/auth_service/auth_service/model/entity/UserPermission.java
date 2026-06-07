package com.auth_service.auth_service.model.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "user_permission")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserPermission extends BaseEntity {

    private UUID userId;
    private UUID permissionId;

}
