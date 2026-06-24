---
phase: 3
title: Harden Teacher Assignment Integration
status: completed
priority: P1
effort: 8h
dependencies:
  - 1
  - 2
---

# Phase 3: Harden Teacher Assignment Integration

## Overview

Add an Auth Service client to Question Service so assignment writes validate a
real active teacher and assignment reads return teacher display information.

## Requirements

- No foreign key across service databases.
- Validate teacher identity before creating/reactivating an assignment.
- Resolve assignment lists in one bulk call.
- Do not persist copied teacher profile fields in Question Service.
- Preserve existing assignment IDs and soft-removal behavior.
- Assignment writes fail closed on Auth timeout, invalid response, missing
  teacher, inactive user, or wrong role.
- Assignment reads preserve inactive teacher rows and expose real identity
  status so Admin can remove stale assignments.

## Architecture

```text
POST subject/{id}/teachers
  -> SubjectTeacherService validates subject
  -> AuthServiceClient.resolveTeachers([teacherId])
  -> require exactly one valid teacher
  -> save/reactivate assignment
  -> return assignment + resolved teacher summary

GET subject/{id}/teachers
  -> load ACTIVE assignments
  -> one bulk resolve call
  -> join in memory by teacherId
  -> return enriched rows, including inactive identity status
```

Use Spring `RestClient`, timeout configuration, and `X-Internal-Api-Key`,
following `exam-service/client/AuthServiceClient.java`. Configuration:

```properties
services.auth.base-url=${AUTH_SERVICE_URL}
services.internal-api-key=${INTERNAL_API_KEY:local-internal-key}
```

## Related Code Files

- Create: `question-service/src/main/java/com/question_service/question_service/config/RestClientConfig.java`
- Create: `question-service/src/main/java/com/question_service/question_service/client/AuthServiceClient.java`
- Create: teacher client contract records under `question-service/.../client/`
- Modify: `question-service/src/main/java/com/question_service/question_service/service/subjects/SubjectTeacherService.java`
- Modify: `question-service/src/main/java/com/question_service/question_service/model/dto/subjects/SubjectTeacherResponseDTO.java`
- Modify: `question-service/src/main/resources/application.properties`
- Modify: `.env.example` or deployment env documentation if present.
- Modify/add tests for client, service, controller, and service security.

## Implementation Steps

1. Port the proven RestClient configuration pattern from Exam Service without
   coupling Question Service to Exam Service code.
2. Implement bulk teacher resolve client with stable mapping for 4xx, 5xx,
   timeout, malformed envelope, and incomplete response.
3. Extend assignment response with `teacherCode`, `fullName`, `displayName`,
   and `email`; retain existing assignment metadata.
4. Validate the requested teacher before any assignment write.
5. Require the subject itself to be `ACTIVE` before assignment writes.
6. Bulk resolve list results and preserve assignment ordering by `assignedAt`.
   Return inactive teachers with display data and `teacherStatus=INACTIVE`.
   For missing/non-teacher legacy references, return an explicit
   `identityState=MISSING|NON_TEACHER` row with the real stored UUID so Admin
   can remove it; never fabricate a name/code.
7. Add tests for active teacher, missing teacher, inactive teacher, non-teacher,
   duplicate assignment, reactivation, Auth outage, malformed response, and
   single-call bulk list enrichment.
8. Update subject-management docs and Bruno collection requests.

## Success Criteria

- [ ] Fake/random UUIDs cannot be assigned.
- [ ] Inactive and non-teacher users cannot be assigned.
- [ ] Assignment list shows names/codes without N+1 calls.
- [ ] Auth outages never create unverified assignments.
- [ ] Inactive or broken legacy assignments remain visible and removable.
- [ ] Archived subjects reject assignment at the backend.
- [ ] No teacher profile snapshot is stored in Question DB.
- [ ] Existing Teacher subject authorization remains unchanged.

## Risk Assessment

Assignment now depends synchronously on Auth Service. This is intentional for
identity integrity. Use short timeouts and clear retryable errors; do not add a
cache until measured traffic justifies it.
