# Admin user management

All browser requests go through Gateway and require an Admin JWT. Auth Service
authorizes the trusted `X-User-*` headers forwarded by Gateway.

The implemented frontend routes are:

```text
/admin/dashboard
/admin/users
/admin/users/:userId
/admin/users/import
```

The user list keeps `keyword`, `role`, `status`, `page`, and `sort` in the URL,
debounces keyword searches, and preserves those filters when returning from a
detail page. Desktop tables and mobile cards render only database-backed
Teacher and Student records.

Base path:

```http
/v1/api/admin/auth-service/users
```

Supported operations:

```http
GET   /users?userType=TEACHER&status=ACTIVE&keyword=an&page=0&size=20&sort=fullName,asc
GET   /users/statistics
GET   /users/{userId}
PUT   /users/{userId}
PATCH /users/{userId}/status
POST  /users/import
```

Search returns only `TEACHER` and `STUDENT` records. Page size is limited to
100. Allowed sort fields are `username`, `studentCode`, `teacherCode`,
`fullName`, `displayName`, `email`, `createdAt`, and `status`.

Profile update accepts only:

```json
{
  "fullName": "Nguyen Van An",
  "displayName": "Thay An",
  "email": "an@example.com",
  "birthDate": "1990-05-20",
  "gender": "MALE"
}
```

Status update accepts only `ACTIVE` or `INACTIVE`:

```json
{"status":"INACTIVE"}
```

Role, username, teacher/student code, password, phone, national ID, and
permissions cannot be changed by these APIs. Admin accounts cannot be listed,
viewed, edited, or deactivated by this feature.

Statistics contain `totalUsers`, `activeUsers`, `inactiveUsers`,
`activeTeachers`, and `activeStudents`. `totalUsers` includes Admin accounts;
`activeUsers` and `inactiveUsers` count only managed Teacher/Student accounts.
Management results never include Admin accounts or unknown legacy statuses.

## Network trust boundary

Auth Service trusts `X-User-*` only inside the private service network. Docker
Compose does not publish Auth or Question Service ports to the host. Browser
and external clients must use Gateway port `8080`; exposing service ports would
allow callers to forge trusted identity headers.

## Internal teacher resolution

Question Service uses:

```http
POST /v1/internal/auth-service/teachers/resolve
X-Internal-Api-Key: <shared key>
```

```json
{"teacherIds":["00000000-0000-0000-0000-000000000001"]}
```

The response preserves request order for real teachers, includes their actual
status, and reports `missingTeacherIds` and `nonTeacherIds`. Requests are
limited to 100 IDs. This endpoint requires `ROLE_INTERNAL` established by the
internal API key and must never be called by a browser.

Stable business errors include `USER_NOT_FOUND`, `INVALID_USER_TYPE`,
`INVALID_USER_STATUS`, and `ADMIN_USER_MANAGEMENT_FORBIDDEN`.

## Frontend data policy

- No Admin user, statistics, import, or teacher-directory data comes from MSW.
- The browser calls only Gateway `/v1/api/admin/**` routes.
- Status changes remain server-authoritative; the UI waits for success before
  refreshing the displayed status.
- Profile editing is limited to `fullName`, `displayName`, `email`,
  `birthDate`, and `gender`.
- Password hashes, encrypted phone/national ID values, blind indexes, and
  permission collections are never rendered.
- Account creation is the Excel import flow; there is no manual single-account
  creation or password form in this workspace.
