---
phase: 2
title: Build Auth Admin User APIs
status: completed
priority: P1
effort: 10h
dependencies:
  - 1
---

# Phase 2: Build Auth Admin User APIs

## Overview

Implement real Admin user discovery, detail, safe profile update, status
management, statistics, and internal teacher resolution in Auth Service.

## Requirements

- Reuse `UserEntity`, `UserRepo`, pagination conventions, response envelope,
  trusted-header authentication, and existing import endpoint.
- Keep import behavior and endpoint backward compatible.
- Search TEACHER/STUDENT users and filter by role/status.
- Return only display-safe fields.
- Validate unique email if repository policy is introduced; otherwise do not
  claim uniqueness.
- Do not decrypt phone/national ID for this Admin slice.
- Internal resolve preserves request order and caps IDs at 100.

## Architecture

Create a general Admin user service instead of duplicating
`StudentDiscoveryService`. Reuse small private helpers or extract pagination
validation only when two real consumers need it.

DTOs:

- `AdminUserSummaryDTO`
- `AdminUserDetailDTO`
- `AdminUserPageResponseDTO`
- `UpdateAdminUserRequestDTO`
- `UpdateUserStatusRequestDTO`
- `AdminUserStatisticsDTO`
- `ResolveTeachersRequestDTO`
- `ResolveTeachersResponseDTO`
- `TeacherSummaryDTO`

Statistics contain `totalUsers`, `activeUsers`, `inactiveUsers`,
`activeTeachers`, and `activeStudents`.
Counts may include Admin accounts only in aggregate `totalUsers`; no Admin
record is returned by management endpoints.

## Related Code Files

- Modify: `auth-service/src/main/java/com/auth_service/auth_service/repository/UserRepo.java`
- Create: `auth-service/src/main/java/com/auth_service/auth_service/controller/AdminUserController.java`
- Create: `auth-service/src/main/java/com/auth_service/auth_service/controller/InternalTeacherController.java`
- Create: `auth-service/src/main/java/com/auth_service/auth_service/service/users/AdminUserService.java`
- Create: `auth-service/src/main/java/com/auth_service/auth_service/service/teachers/TeacherResolutionService.java`
- Create/modify: `auth-service/src/main/java/com/auth_service/auth_service/model/dto/users/*`
- Create: `auth-service/src/main/java/com/auth_service/auth_service/model/dto/teachers/*`
- Modify: `auth-service/src/main/java/com/auth_service/auth_service/config/security/SecurityConfig.java`
- Add tests under matching `auth-service/src/test/java/...` packages.

## Implementation Steps

1. Add repository query supporting optional `UserType`, status, normalized
   keyword, pageable sorting, and count methods for statistics.
2. Implement page/sort/status/type validation. Allow sort only by `username`,
   `studentCode`, `teacherCode`, `fullName`, `displayName`, `email`,
   `createdAt`, and `status`.
3. Map entities to safe summary/detail DTOs. Never serialize `UserEntity`.
4. Implement detail lookup and safe profile update. Reject ADMIN targets.
   Validate required full name, optional email format, birth date, and gender.
5. Implement status transition. Reject unknown statuses and ADMIN targets.
6. Implement statistics with repository counts; avoid loading full tables.
7. Implement internal bulk teacher resolution using the existing
   `findAllByIdIn` pattern and explicit rejection buckets.
8. Add controller unit/security tests proving Admin-only and internal-only
   access, pagination limits, filters, response redaction, Admin-target
   rejection, and resolve ordering.
9. Keep `AdminUserImportController` under the same base path and run import
   regression tests.

## Success Criteria

- [ ] Admin can search teachers/students and inspect safe details.
- [ ] Admin can update allowed profile fields and status.
- [ ] Admin accounts cannot be listed, edited, or deactivated here.
- [ ] Statistics use count queries.
- [ ] Internal resolve distinguishes missing, inactive, and wrong-role IDs.
- [ ] No sensitive entity fields appear in JSON.
- [ ] Existing login, import, and student-discovery tests still pass.

## Risk Assessment

- Free-form status values in existing data can break filters. Normalize known
  values and treat unknown rows as invalid legacy data, not active users.
- Broad entity serialization is a severe data-leak risk. DTO mapping is
  mandatory.
- Large unbounded searches are prevented by maximum page size and sort allowlist.

## Security Considerations

Never accept acting Admin ID from request body or query parameters. Authorization
comes from the trusted principal; target-role restrictions come from persisted
user data.
