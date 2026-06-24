# Import tài khoản từ Excel

Tính năng này cho phép quản trị viên tạo nhiều tài khoản học sinh hoặc giáo
viên từ một tệp `.xlsx`. Giao diện được triển khai tại
`/admin/users/import` và luôn gửi dữ liệu thật qua Gateway; không có Admin MSW
handler hoặc kết quả import giả lập.

## Endpoint

```http
POST /v1/api/admin/auth-service/users/import
Authorization: Bearer <admin-token>
Content-Type: multipart/form-data
```

Multipart field:

```text
file: users.xlsx
```

Trong môi trường local, Gateway mặc định ở `http://localhost:8080`. Browser
không gọi trực tiếp Auth Service. Gateway xác thực JWT `ADMIN`, loại bỏ các
`X-User-*` do client tự gửi và tạo trusted headers cho service phía sau.

Frontend chỉ cho chọn `.xlsx`, hiển thị tên và kích thước tệp, khóa thao tác khi
đang gửi, rồi hiển thị đúng thống kê và toàn bộ lỗi theo dòng do máy chủ trả về.
Máy chủ vẫn là nguồn xác thực định dạng và nội dung cuối cùng. Request import
dùng timeout riêng 60 giây; timeout mặc định của API client không bị thay đổi.

## Cấu trúc tệp Excel

Sheet đầu tiên được sử dụng. Dòng đầu tiên là header.

| Header | Bắt buộc | Ghi chú |
| --- | --- | --- |
| `username` | Không | Giá trị bị bỏ qua; hệ thống sinh từ mã học sinh/giáo viên. |
| `password` | Không | Giá trị bị bỏ qua; hệ thống sinh từ mã học sinh/giáo viên. |
| `fullName` | Có | Họ tên đầy đủ. |
| `email` | Không | Nếu có, phải là email hợp lệ. |
| `studentCode` | Có với học sinh | Không được nhập cùng `teacherCode`. |
| `teacherCode` | Có với giáo viên | Không được nhập cùng `studentCode`. |
| `phone` | Không | Được mã hóa và tạo blind index trước khi lưu. |
| `nationalId` | Không | Được mã hóa và tạo blind index trước khi lưu. |
| `birthDate` | Không | Định dạng `yyyy-MM-dd`, ví dụ `2004-01-15`. |
| `gender` | Không | Một trong `MALE`, `FEMALE`, `OTHER`. |

Các cột mã, số điện thoại và căn cước nên dùng định dạng `Text` trong Excel để
không mất số `0` ở đầu.

## Quy tắc tạo tài khoản

```text
Học sinh: username = password ban đầu = studentCode, userType = STUDENT
Giáo viên: username = password ban đầu = teacherCode, userType = TEACHER
```

Mật khẩu được băm bằng `PasswordEncoder`; hệ thống không lưu mật khẩu dạng
plain text. Quy tắc mật khẩu mặc định này phù hợp với phạm vi dự án hiện tại và
cần được thay thế bằng luồng kích hoạt/đổi mật khẩu trong hệ thống production
có yêu cầu bảo mật cao hơn.

## Partial success

- Dòng hợp lệ được lưu vào database.
- Dòng lỗi bị bỏ qua.
- Một dòng lỗi không rollback các dòng đã tạo thành công.
- Response trả tổng số dòng, số thành công, số thất bại và mọi lỗi theo dòng.

```json
{
  "timestamp": "2026-06-22T09:00:00",
  "status": 200,
  "message": "Success",
  "data": {
    "totalRows": 3,
    "successCount": 2,
    "failedCount": 1,
    "errors": [
      {
        "rowNumber": 4,
        "fieldName": "studentCode",
        "errorCode": "DUPLICATE_STUDENT_CODE",
        "message": "Student code already exists"
      }
    ]
  }
}
```

## Mã lỗi phổ biến

| Error code | Ý nghĩa |
| --- | --- |
| `REQUIRED` | Thiếu trường bắt buộc. |
| `MISSING_CODE` | Thiếu cả mã học sinh và mã giáo viên. |
| `INVALID_CODE` | Một dòng có cả mã học sinh và mã giáo viên. |
| `DUPLICATE_USERNAME_IN_FILE` | Username trùng trong tệp. |
| `DUPLICATE_STUDENT_CODE_IN_FILE` | Mã học sinh trùng trong tệp. |
| `DUPLICATE_TEACHER_CODE_IN_FILE` | Mã giáo viên trùng trong tệp. |
| `DUPLICATE_USERNAME` | Username đã tồn tại trong database. |
| `DUPLICATE_STUDENT_CODE` | Mã học sinh đã tồn tại trong database. |
| `DUPLICATE_TEACHER_CODE` | Mã giáo viên đã tồn tại trong database. |
| `INVALID_EMAIL` | Email không hợp lệ. |
| `INVALID_BIRTH_DATE` | Ngày sinh không đúng `yyyy-MM-dd`. |
| `INVALID_GENDER` | Gender không thuộc enum hỗ trợ. |
| `CREATE_USER_FAILED` | Lỗi khi mã hóa hoặc lưu người dùng. |

