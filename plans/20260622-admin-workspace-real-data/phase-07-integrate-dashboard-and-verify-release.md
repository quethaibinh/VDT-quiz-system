---
phase: 7
title: Integrate Dashboard and Verify Release
status: completed
priority: P1
effort: 4h
dependencies:
  - 5
  - 6
---

# Phase 7: Integrate Dashboard and Verify Release

## Overview

Compose a real-data dashboard, complete end-to-end verification, update docs,
and remove any Admin runtime mock paths before release.

## Requirements

- Dashboard cards use Auth statistics and Question subject list.
- Show only metrics available from real APIs.
- Quick actions link to import, users, and subjects.
- Partial endpoint failure does not hide all dashboard content.
- No fabricated recent activity or “items needing attention”.
- Existing Teacher test/build gates remain green.

## Architecture

Run Auth statistics and subject queries in parallel. Render each dashboard
section independently so one service outage produces a scoped retry state.
Derived subject counts are computed from the returned real list; do not store
them as frontend state.

## Related Code Files

- Create: `frontend/src/features/admin/dashboard/pages/admin-dashboard-page.tsx`
- Create dashboard query/component/test files as needed.
- Modify: `frontend/src/app/router.tsx`
- Inspect: `frontend/src/mocks/handlers.ts` to ensure Admin business handlers
  are absent; existing Teacher mocks remain untouched.
- Update: `frontend/README.md`
- Update: `docs/frontend/FRONTEND_PRODUCT_DESIGN.md`
- Update: `docs/frontend/WIREFRAMES.md`
- Update: `docs/ADMIN_USER_MANAGEMENT.md`
- Update: `docs/SUBJECT_MANAGEMENT.md`
- Add/update Bruno requests for all public/internal contracts.

## Implementation Steps

1. Implement dashboard cards: total active users, active teachers, active
   students, active subjects, and archived subjects.
2. Add independent loading/error/retry states and real quick actions.
3. Run backend unit, controller, security, client, and integration tests.
4. Run frontend lint, typecheck, Vitest, and production build.
5. Run Gateway smoke flow with Admin JWT:
   login, user search/detail/update/status, import, subject CRUD, teacher search,
   assign/list/remove.
6. Verify Teacher JWT gets 403 for Admin routes; unauthenticated requests get
   401; spoofed `X-User-*` headers cannot elevate permissions.
7. Verify browser CORS preflight for GET/POST/PUT/PATCH/DELETE through Gateway.
8. Test responsive layouts at 360px, 768px, and 1440px plus keyboard navigation.
9. Search frontend source for Admin MSW handlers, hard-coded teacher IDs, and
   direct service URLs; release only when none remain.
10. Update API/design documentation with final controller paths and screenshots
    only if UI matches source-of-truth wireframes.

## Success Criteria

- [ ] Dashboard contains only real or real-derived values.
- [ ] Full Admin workflow succeeds through Gateway.
- [ ] Role isolation and trusted-header protections are verified.
- [ ] No runtime Admin mock data or hard-coded identity IDs remain.
- [ ] Auth, Question, Gateway, and frontend verification commands pass.
- [ ] Teacher workspace has no regression.
- [ ] Documentation matches implemented paths and behavior.

## Risk Assessment

End-to-end verification can expose stale environment variables or service-port
assumptions. Treat Gateway `8080` as the only browser entry point and document
required `AUTH_SERVICE_URL`, `INTERNAL_API_KEY`, and frontend base URL values.
