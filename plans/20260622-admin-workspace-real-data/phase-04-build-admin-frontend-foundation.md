---
phase: 4
title: Build Admin Frontend Foundation
status: completed
priority: P1
effort: 7h
dependencies:
  - 1
---

# Phase 4: Build Admin Frontend Foundation

## Overview

Add role-aware routing and a responsive Admin workspace while preserving the
existing Teacher journey and sharing only stable shell components.

## Requirements

- Login redirects ADMIN to `/admin/dashboard` and TEACHER to
  `/teacher/subjects`.
- Unauthenticated routes preserve `returnTo`.
- Cross-role access returns `/forbidden`.
- Admin feature code is isolated under `src/features/admin/`.
- Shared shell extraction must not change Teacher navigation behavior.
- No Admin MSW handlers or runtime mode switches; existing Teacher mocks remain.

## Architecture

```text
src/features/admin/
  layout/
  dashboard/
  users/
  imports/
  subjects/
  assignments/

src/components/shared/
  workspace-shell.tsx
  existing reusable feedback/navigation primitives
```

Replace the fixed `TeacherGuard` implementation with a generic `RoleGuard`
used by small `AdminGuard`/`TeacherGuard` wrappers or route props. Extract
`WorkspaceShell` only for sidebar/topbar/mobile drawer structure; domain
navigation remains feature-owned.

## Related Code Files

- Modify: `frontend/src/app/router.tsx`
- Modify: `frontend/src/features/auth/pages/login-page.tsx`
- Create: `frontend/src/features/auth/components/role-guard.tsx`
- Modify: `frontend/src/features/auth/components/teacher-guard.tsx`
- Create: `frontend/src/features/auth/components/admin-guard.tsx`
- Create: `frontend/src/components/shared/workspace-shell.tsx`
- Modify: `frontend/src/features/teacher/layout/teacher-layout.tsx`
- Create: `frontend/src/features/admin/layout/admin-layout.tsx`
- Create: `frontend/src/features/admin/layout/admin-navigation.ts`
- Add route, guard, navigation, and shell tests.

## Implementation Steps

1. Add regression tests for current Teacher guard, navigation, sign-out, and
   mobile drawer behavior before shell extraction.
2. Implement role-to-home mapping for ADMIN and TEACHER; STUDENT remains
   forbidden/not-yet-supported rather than being sent into Teacher UI.
3. Implement generic role guard with full pathname + search `returnTo`.
4. Extract shared workspace shell through explicit props for label, home path,
   navigation items, user role label, and children outlet.
5. Migrate Teacher layout to shared shell and keep its navigation tests green.
6. Add Admin layout/navigation for Tổng quan, Người dùng, Import tài khoản,
   and Môn học.
7. Register Admin routes with placeholder route components only when their
   implementation phase immediately follows; no fake data.
8. Verify keyboard focus, mobile drawer dismissal, active route state, and
   Vietnamese labels.

## Success Criteria

- [ ] ADMIN and TEACHER land in the correct workspace after login.
- [ ] Each role is rejected from the other workspace.
- [ ] Teacher routes and layout retain existing behavior.
- [ ] Admin sidebar works at mobile and desktop widths.
- [ ] Shared components contain no Admin/Teacher API logic.
- [ ] Runtime does not register Admin mock handlers.

## Risk Assessment

Refactoring the Teacher shell can create broad visual regressions. Keep the
shared shell narrow, protect behavior with tests, and avoid redesigning Teacher
pages in this phase.
