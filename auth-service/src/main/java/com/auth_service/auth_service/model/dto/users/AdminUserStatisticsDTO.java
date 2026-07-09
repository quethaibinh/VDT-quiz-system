package com.auth_service.auth_service.model.dto.users;

public record AdminUserStatisticsDTO(
        long totalUsers,
        long activeUsers,
        long inactiveUsers,
        long activeTeachers,
        long activeStudents
) {
}
