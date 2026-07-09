# Quản lý môn học và phân công giáo viên

Admin quản lý danh mục môn học và phân công giáo viên trong Question Service.
Giáo viên chỉ nhìn thấy môn đang hoạt động mà mình có assignment `ACTIVE`.

Mọi request từ browser phải đi qua Gateway với Admin JWT. Auth Service và
Question Service không phải public browser entry point vì các service tin
trusted identity headers do Gateway tạo.

## Frontend routes

```text
/admin/subjects
/admin/subjects/:subjectId
```

Danh sách hỗ trợ tìm theo mã/tên và lọc `ACTIVE`/`ARCHIVED`. Admin có thể tạo,
sửa, lưu trữ và khôi phục môn học. Môn đã lưu trữ vẫn hiển thị assignment hiện
tại nhưng không cho phân công mới.

## Admin Subject APIs

```http
POST  /v1/api/admin/question-service/subjects
GET   /v1/api/admin/question-service/subjects?status=ACTIVE&keyword=phi
GET   /v1/api/admin/question-service/subjects/{subjectId}
PUT   /v1/api/admin/question-service/subjects/{subjectId}
PATCH /v1/api/admin/question-service/subjects/{subjectId}/archive
PATCH /v1/api/admin/question-service/subjects/{subjectId}/restore
```

Payload tạo/cập nhật:

```json
{
  "code": "PHI101",
  "name": "Triết học đại cương",
  "description": "Môn học nhập môn"
}
```

## Teacher assignment

```http
GET    /v1/api/admin/question-service/subjects/{subjectId}/teachers
POST   /v1/api/admin/question-service/subjects/{subjectId}/teachers
DELETE /v1/api/admin/question-service/subjects/{subjectId}/teachers/{teacherId}
```

Admin không nhập UUID thủ công. Teacher picker gọi API người dùng thật:

```http
GET /v1/api/admin/auth-service/users
    ?userType=TEACHER
    &status=ACTIVE
    &keyword=<name-code-email>
    &page=0
    &size=20
    &sort=fullName,asc
```

Khi Admin chọn một kết quả, frontend gửi ID thật của record đó:

```json
{"teacherId":"<selected-real-teacher-id>"}
```

Teacher đã được phân công bị vô hiệu hóa trong picker. Question Service xác
minh lại teacher qua internal Auth contract trước khi ghi; chỉ teacher thật,
role `TEACHER`, status `ACTIVE` mới được phân công vào subject `ACTIVE`. Nếu
Auth timeout, response sai, teacher không tồn tại, sai role hoặc đã inactive,
write thất bại và không tạo assignment.

Assignment list được resolve theo lô, không N+1, và trả:

- `teacherCode`, `fullName`, `displayName`, `email`
- `teacherStatus`
- `identityState`

Teacher đã inactive vẫn hiển thị và có thể gỡ. Reference cũ bị hỏng hiển thị
`identityState=MISSING` hoặc `NON_TEACHER`; UI chỉ cho gỡ, không tạo tên/mã giả.
DELETE soft-removes assignment bằng status `INACTIVE`.

Không có Admin MSW teacher directory, fallback ID hoặc hard-coded teacher UUID.

## Internal teacher resolution

Question Service gọi nội bộ:

```http
POST /v1/internal/auth-service/teachers/resolve
X-Internal-Api-Key: <shared-key>
```

Endpoint này chỉ dành cho service-to-service, yêu cầu `ROLE_INTERNAL`, không
được gọi từ browser.

## Teacher Subject APIs

```http
GET /v1/api/question-service/teacher/subjects
GET /v1/api/question-service/teacher/subjects/{subjectId}
```

Teacher import câu hỏi hoặc quản lý nội dung theo subject sau khi assignment đã
được xác thực. Subject không tồn tại, đã archive hoặc chưa được phân công sẽ bị
từ chối trước khi xử lý nghiệp vụ tiếp theo.

## Error codes thường gặp

- `SUBJECT_NOT_FOUND`
- `SUBJECT_NOT_ACTIVE`
- `DUPLICATE_SUBJECT_CODE`
- `SUBJECT_FORBIDDEN`
- `TEACHER_ALREADY_ASSIGNED`
- `TEACHER_NOT_FOUND`
- `TEACHER_NOT_ACTIVE`
- `USER_NOT_TEACHER`
- `AUTH_SERVICE_UNAVAILABLE`
- `INVALID_AUTH_SERVICE_RESPONSE`
- `TEACHER_SUBJECT_ASSIGNMENT_NOT_FOUND`
- `REQUIRED_FIELD`
