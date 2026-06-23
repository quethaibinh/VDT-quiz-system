---
title: Admin Workspace With Real Data
description: >-
  Build an end-to-end Admin workspace backed only by real Auth and Question
  Service data, including user administration, subject management, and verified
  teacher assignment.
status: completed
priority: P1
effort: 48h
branch: feature/exam-service
tags:
  - feature
  - frontend
  - backend
  - api
  - auth
  - security
blockedBy: []
blocks: []
created: '2026-06-22T09:52:34.491Z'
createdBy: 'ck:plan'
source: skill
mode: hard
---

# Admin Workspace With Real Data

## Overview

Build the Admin workspace inside the existing React application. Existing
import and subject APIs remain the source of truth. Add missing Auth Service
user discovery/detail/profile/status/statistics APIs and an internal teacher
resolution contract. Harden Question Service assignment so only real active
teachers can be assigned and assignment responses contain display data.

No Admin runtime business data comes from MSW. Existing Teacher development
mocks remain outside this scope. The frontend combines real Auth statistics
and Question subject data for the dashboard instead of introducing an Admin
BFF.

## Scope

- Admin role routing, guard, responsive shell, and Sahara design reuse.
- Real user list, filters, detail, safe profile editing, activation/deactivation.
- Existing real Excel import flow.
- Existing real subject CRUD, archive, and restore flow.
- Real teacher search and verified assign/remove flow.
- Real dashboard counts and quick actions.
- Backend, controller, security, frontend, and integration tests.

## Out of Scope

- Student workspace.
- Password reset, role changes, permission matrix, audit-log timeline.
- Managing ADMIN accounts; this workspace manages TEACHER and STUDENT records.
- Manual single-user password creation; account creation stays Excel-based.
- Realtime dashboard, new message broker, BFF, or cross-service database joins.
- Mock-backed Admin screens in development or production.

## Architecture

```text
Admin browser
  -> Gateway /v1/api/admin/**
     -> Auth Service: users, import, statistics
     -> Question Service: subjects, assignments
          -> Auth Service internal teacher resolve
```

- Browser calls Gateway only.
- Question Service stores only teacher UUID assignment references.
- Auth Service owns teacher identity and validates role/status.
- Assignment writes fail closed if Auth Service cannot verify the teacher.
- Assignment reads resolve identities/status in one bulk call; no N+1 requests.
- Inactive assigned teachers stay visible and removable; only new assignment
  requires `ACTIVE`.
- Frontend feature code lives under `src/features/admin/`.
- Truly reusable shell/primitives stay under `src/components/shared/`.

## Contract Summary

```http
GET   /v1/api/admin/auth-service/users
GET   /v1/api/admin/auth-service/users/statistics
GET   /v1/api/admin/auth-service/users/{userId}
PUT   /v1/api/admin/auth-service/users/{userId}
PATCH /v1/api/admin/auth-service/users/{userId}/status
POST  /v1/api/admin/auth-service/users/import

POST  /v1/internal/auth-service/teachers/resolve

POST   /v1/api/admin/question-service/subjects
GET    /v1/api/admin/question-service/subjects
GET    /v1/api/admin/question-service/subjects/{subjectId}
PUT    /v1/api/admin/question-service/subjects/{subjectId}
PATCH  /v1/api/admin/question-service/subjects/{subjectId}/archive
PATCH  /v1/api/admin/question-service/subjects/{subjectId}/restore
GET    /v1/api/admin/question-service/subjects/{subjectId}/teachers
POST   /v1/api/admin/question-service/subjects/{subjectId}/teachers
DELETE /v1/api/admin/question-service/subjects/{subjectId}/teachers/{teacherId}
```

## Execution Order

Phases 1-3 establish contracts and trusted backend data. Phase 4 can begin
after Phase 1, but phases 5-7 require their backend contracts. Do not wire
frontend mutations to placeholder IDs.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Lock Admin Contracts and Security](./phase-01-lock-admin-contracts-and-security.md) | Completed |
| 2 | [Build Auth Admin User APIs](./phase-02-build-auth-admin-user-apis.md) | Completed |
| 3 | [Harden Teacher Assignment Integration](./phase-03-harden-teacher-assignment-integration.md) | Completed |
| 4 | [Build Admin Frontend Foundation](./phase-04-build-admin-frontend-foundation.md) | Completed |
| 5 | [Implement Admin User and Import UI](./phase-05-implement-admin-user-and-import-ui.md) | Completed |
| 6 | [Implement Subject and Assignment UI](./phase-06-implement-subject-and-assignment-ui.md) | Completed |
| 7 | [Integrate Dashboard and Verify Release](./phase-07-integrate-dashboard-and-verify-release.md) | Completed |

## Dependencies

- Reuses completed `20260605-auth-excel-import`.
- Reuses completed `20260609-subject-admin-teacher-assignment`.
- Reuses completed `20260610-teacher-react-frontend`.
- No dependency on unfinished Exam plans; avoid touching their contracts.

## Release Gates

```powershell
cd auth-service; .\mvnw.cmd test
cd question-service; .\mvnw.cmd test
cd gateway; .\mvnw.cmd test
cd frontend; npm run lint
cd frontend; npm run typecheck
cd frontend; npm run test
cd frontend; npm run build
```

Gateway smoke tests must use an Admin JWT and prove Teacher tokens receive 403
for every new Admin route.

## Key Risks

- Cross-service Auth outage during assignment: fail closed; return stable error.
- Existing Gateway/Auth CORS omit `PATCH`: add `PATCH` before browser status and
  archive flows are considered complete.
- Existing docs contain stale endpoint paths: controller paths are authoritative.
- User status is free-form string today: validate allowed values at service edge.
- Existing frontend text shows encoding damage in shell files: preserve UTF-8
  and verify Vietnamese labels in browser tests/build output.

## Red Team Review

- Restricted user administration to TEACHER/STUDENT; prevents accidental Admin
  account governance and removes self-lockout scope.
- Changed assignment reads to tolerate real inactive/missing identity states so
  Admin can clean stale assignments. Assignment writes still fail closed.
- Required backend active-subject validation; UI disabling alone is insufficient.
- Kept existing Teacher development mocks untouched while banning Admin runtime
  mock handlers and IDs.
- Rejected BFF, identity caching, copied teacher profile columns, and audit-log
  work as unnecessary for this release.

## Validation Log

- Tier: Full, 7 phases.
- Verified current controller paths, Gateway Admin matcher, Auth internal
  matcher, UserRepo/student discovery pattern, SubjectTeacherService behavior,
  frontend router/guard/layout, API envelope, and MSW bootstrap.
- Whole-plan consistency sweep: 8 files reread; 4 decision deltas reconciled;
  unresolved contradictions: 0.
