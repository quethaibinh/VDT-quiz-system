---
phase: 1
title: Lock Admin Contracts and Security
status: completed
priority: P1
effort: 5h
dependencies: []
---

# Phase 1: Lock Admin Contracts and Security

## Overview

Freeze public/internal contracts, authorization rules, error codes, and
frontend data boundaries before implementation. Update design documentation so
no screen depends on fabricated data.

## Requirements

- Public Admin routes require `ROLE_ADMIN` at Gateway and target service.
- Internal teacher resolution requires `ROLE_INTERNAL` and internal API key.
- Browser never calls internal routes or service ports directly.
- User profile responses never expose password hash, encrypted phone, national
  ID, blind indexes, or permissions not needed by the UI.
- Allowed account statuses: `ACTIVE`, `INACTIVE`.
- Role and student/teacher code changes are out of scope for profile update.
- Public Admin user management excludes users with role `ADMIN`.

## Architecture

Public user search contract:

```http
GET /v1/api/admin/auth-service/users
    ?userType=TEACHER|STUDENT
    &status=ACTIVE|INACTIVE
    &keyword=
    &page=0
    &size=20
    &sort=fullName,asc
```

Search matches username, code, full name, display name, and email. Enforce a
maximum page size of 100 and a sort-field allowlist.

Safe update contract:

```json
{
  "fullName": "Nguyen Van An",
  "displayName": "Thay An",
  "email": "an@example.com",
  "birthDate": "1990-05-20",
  "gender": "MALE"
}
```

Status contract:

```http
PATCH /v1/api/admin/auth-service/users/{userId}/status
{"status":"INACTIVE"}
```

List/detail/update/status operations reject ADMIN targets. Admin identity
management is a separate security-sensitive feature.

Internal teacher resolution returns ordered teacher summaries including their
real status, plus explicit `missingTeacherIds` and `nonTeacherIds`. Assignment
writes require a resolved `ACTIVE` teacher; reads may display inactive teachers.

## Related Code Files

- Modify: `docs/frontend/FRONTEND_PRODUCT_DESIGN.md`
- Modify: `docs/frontend/WIREFRAMES.md`
- Modify: `docs/SUBJECT_MANAGEMENT.md`
- Create: `docs/ADMIN_USER_MANAGEMENT.md`
- Modify: `gateway/src/main/resources/application.yml`
- Review: `gateway/src/main/java/com/gateway/gateway/config/SecurityConfig.java`
- Review: `auth-service/src/main/java/com/auth_service/auth_service/config/security/SecurityConfig.java`
- Review: `question-service/src/main/java/com/question_service/question_service/config/security/SecurityConfig.java`

## Implementation Steps

1. Document exact request/response DTOs, pagination shape, status transitions,
   sort allowlists, and stable business error codes.
2. Define Admin routes as `/v1/api/admin/{service}/...`; mark older doc examples
   such as `/v1/api/auth-service/admin/...` stale.
3. Define `USER_NOT_FOUND`, `INVALID_USER_TYPE`, `INVALID_USER_STATUS`,
   `ADMIN_USER_MANAGEMENT_FORBIDDEN`, `TEACHER_NOT_FOUND`, `TEACHER_NOT_ACTIVE`,
   `USER_NOT_TEACHER`, `AUTH_SERVICE_UNAVAILABLE`, and
   `INVALID_AUTH_SERVICE_RESPONSE`.
4. Add `PATCH` to Gateway and Auth Service CORS allowlists; verify Question
   Service already needs the same correction for subject archive/restore.
5. Update frontend design docs: all Admin runtime data is real; remove planned
   mock assignment/user-directory language.
6. Record UI route map:
   `/admin/dashboard`, `/admin/users`, `/admin/users/:userId`,
   `/admin/users/import`, `/admin/subjects`, `/admin/subjects/:subjectId`.

## Success Criteria

- [ ] Public and internal contracts are documented with examples.
- [ ] No design document tells developers to mix mock and real Admin IDs.
- [ ] CORS design includes every method used by Admin UI.
- [ ] Security ownership and sensitive-field exclusions are explicit.
- [ ] Scope excludes role/code/password management without ambiguity.
- [ ] Admin-account management is explicitly rejected by API and UI scope.

## Risk Assessment

Main risk is accidental scope expansion into a full identity-management
platform. Keep update fields narrow and leave credential flows untouched.
