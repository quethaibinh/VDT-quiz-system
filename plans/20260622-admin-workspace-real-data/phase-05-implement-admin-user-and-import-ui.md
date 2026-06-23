---
phase: 5
title: Implement Admin User and Import UI
status: completed
priority: P1
effort: 7h
dependencies:
  - 2
  - 4
---

# Phase 5: Implement Admin User and Import UI

## Overview

Build real user administration and Excel import screens against Auth Service.
Account creation remains the existing import workflow.

## Requirements

- User list filters live in URL: keyword, role, status, page, sort.
- Debounce keyword search; keep previous page data during transitions.
- Detail page supports safe profile edit and activate/deactivate.
- Deactivation requires confirmation containing the user name/code.
- ADMIN records are not exposed or mutable in this workspace.
- Import shows selected file, progress/pending state, summary, and row errors.
- File selection accepts `.xlsx`; server remains validation authority.

## Architecture

```text
features/admin/users/
  api/admin-user-api.ts
  model/admin-user-contracts.ts
  model/admin-user-queries.ts
  components/
  pages/

features/admin/imports/
  api/admin-user-import-api.ts
  model/import-contracts.ts
  pages/user-import-page.tsx
```

Use TanStack Query for server state, React Hook Form + Zod for edit forms, and
existing `PageHeader`, `FilterBar`, `DataState`, `Pagination`,
`ConfirmDialog`, inputs, buttons, and status chips.

## Related Code Files

- Create files under `frontend/src/features/admin/users/`.
- Create files under `frontend/src/features/admin/imports/`.
- Modify: `frontend/src/app/router.tsx`
- Reuse/extend shared UI only where behavior has two real consumers.
- Add API adapter, page, form, filter, status mutation, and import tests.

## Implementation Steps

1. Define TypeScript contracts matching Auth DTOs and shared `PageResponse`.
2. Implement API functions for list, statistics, detail, update, status, and
   multipart import using `unwrap`.
3. Build responsive user list: desktop table, mobile cards, role/status chips,
   search, filters, sorting, pagination, loading, empty, error, and retry.
4. Build user detail page with immutable identity section and editable safe
   profile form.
5. Add activate/deactivate confirmation, optimistic-button locking, query
   invalidation, and stable error feedback. Do not optimistically change
   account status before server success.
6. Build Excel import UI from existing wireframe and real partial-success
   contract. Display every returned row error without inventing results.
7. Add navigation from list to import and detail; preserve list filters when
   returning.
8. Test request params, multipart field name, mutation invalidation,
   Admin-target exclusion, responsive semantic content, and error states.

## Success Criteria

- [ ] User list and detail display database-backed records only.
- [ ] Filters, sort, and pagination call the real API contract.
- [ ] Allowed profile fields update and re-render after invalidation.
- [ ] Status actions are confirmed and server-authoritative.
- [ ] Import sends field `file` and renders partial-success errors accurately.
- [ ] No password hash, encrypted identity data, or permissions are rendered.

## Risk Assessment

The existing API client timeout is 20 seconds; large imports may exceed it.
Measure current import behavior before introducing a separate timeout. If
needed, set timeout only on the import request rather than globally.
