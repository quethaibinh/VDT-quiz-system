---
phase: 6
title: Implement Subject and Assignment UI
status: completed
priority: P1
effort: 7h
dependencies:
  - 3
  - 4
---

# Phase 6: Implement Subject and Assignment UI

## Overview

Build real subject lifecycle and teacher-assignment UI against Question Service
and Auth-backed enriched assignment contracts.

## Requirements

- List/search/filter active and archived subjects.
- Create and edit code, name, description.
- Confirm archive/restore actions.
- Subject detail displays assigned teachers with names and codes.
- Inactive assigned teachers remain visible with status and a remove action.
- Teacher picker searches only active teachers through Auth Admin API.
- Already assigned teachers are disabled/excluded from picker.
- Assign/remove mutations use real UUIDs from real API responses.

## Architecture

Admin subjects own their own API/types despite structural similarity to Teacher
subjects because paths, permissions, fields, and mutations differ. Share visual
components, not domain repositories.

Teacher picker flow:

```text
search input
 -> GET Admin Auth users?userType=TEACHER&status=ACTIVE
 -> select real teacher
 -> POST Question assignment with selected UUID
 -> invalidate subject assignments and relevant dashboard data
```

## Related Code Files

- Create files under `frontend/src/features/admin/subjects/`.
- Create files under `frontend/src/features/admin/assignments/`.
- Modify: `frontend/src/app/router.tsx`
- Reuse: `frontend/src/components/shared/confirm-dialog.tsx`
- Reuse: `frontend/src/components/shared/data-state.tsx`
- Reuse: `frontend/src/components/shared/filter-bar.tsx`
- Add API, query, form, picker, list, and detail tests.

## Implementation Steps

1. Define Admin subject and enriched assignment contracts.
2. Implement subject list/detail/create/update/archive/restore API adapters.
3. Build subject list with keyword/status filters, responsive rows/cards, and
   create/edit drawer or dialog following existing wireframe.
4. Build subject detail with metadata, lifecycle actions, and assignment section.
5. Implement debounced real teacher search with pagination and explicit empty,
   loading, and Auth unavailable states.
6. Assign selected teacher UUID and render returned enriched assignment.
7. Remove assignment only after confirmation; invalidate assignment and subject
   queries after success.
8. Disable assignment controls when subject is archived, while still showing
   existing assignments.
9. Render missing/non-teacher legacy references as explicit integrity states
   using the stored real UUID, with remove as the only mutation.
10. Test duplicate subject code, archived subject behavior, duplicate assignment,
   inactive teacher race, Auth outage, and mutation retries.

## Success Criteria

- [ ] Subject lifecycle works through existing real endpoints.
- [ ] Admin selects teachers by name/code, never types UUID manually.
- [ ] Every assigned teacher was verified by Auth Service.
- [ ] Assignment list renders enriched real identity data.
- [ ] Archived subjects cannot receive new assignments.
- [ ] Inactive/broken assignment identities can be identified and removed.
- [ ] No mock teacher directory or fallback IDs exist.

## Risk Assessment

A teacher may become inactive after appearing in search but before assignment.
Question Service revalidates at mutation time; frontend surfaces the server
error and refreshes search results.
