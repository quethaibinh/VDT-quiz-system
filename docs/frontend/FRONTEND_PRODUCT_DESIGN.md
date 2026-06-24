# Quiz Platform Frontend Product Design

## 1. Muc tieu

Thiet ke frontend React cho nen tang thi trac nghiem, bao phu ba vai tro:

- Quan tri vien: quan ly tai khoan, mon hoc va phan cong giao vien.
- Giao vien: quan ly cau hoi, bo cau hoi, ca thi, giam sat va ket qua.
- Hoc sinh: xem ca thi, vao phong cho, lam bai va xem ket qua.

Frontend uu tien API da ton tai. Chuc nang chua co backend van duoc thiet ke de
giu luong san pham lien mach, nhung phai duoc danh dau `Planned API`.

## 2. Pham vi va tieu chi chap nhan

### Dau ra

- Sitemap va route map.
- User flow cho Admin, Teacher va Student.
- Design system dua tren `DESIGN.md`.
- Wireframe cho tat ca man hinh trong pham vi.
- Tam mockup dai dien.
- Bang doi chieu man hinh voi API hien tai va API can bo sung.
- De xuat kien truc React va responsive behavior.

### Tieu chi chap nhan

- Toan bo noi dung giao dien bang tieng Viet.
- Ho tro desktop, tablet va mobile.
- Khong de lo dap an dung trong luong lam bai cua hoc sinh.
- Teacher workflow luon co subject context khi quan ly cau hoi.
- Man hinh co API that phai dung dung path controller hien tai.
- Man hinh chua co API phai co trang thai empty/loading/error va mock contract.
- Luong thi uu tien tinh an toan: dong ho server, auto-save, offline recovery,
  idempotent submit va xac nhan nop bai.

### Ngoai pham vi vong thiet ke

- Scaffold project React.
- Viet component hoac test.
- Sua backend.
- Anti-cheat bang webcam hoac nhan dien khuon mat.

## 3. Huong thiet ke da chon

### Mot design system, ba workspace theo vai tro

Dung chung token, component va interaction pattern. Moi vai tro co navigation va
dashboard rieng. Man hinh lam bai dung layout focus rieng, khong tai su dung
dashboard shell.

Ly do:

- KISS: mot frontend deployable, mot he thong component.
- DRY: form, table, filter, dialog, feedback dung chung.
- Van tach ro mental model cua Admin, Teacher va Student.
- De them API trong tuong lai ma khong phai doi lai navigation.

Khong chon mot dashboard dong duy nhat vi luong lam bai khac xa luong quan tri.
Khong chon ba ung dung rieng vi lam tang chi phi build, auth va component.

## 4. Information architecture

```text
/
|-- /login
|-- /forbidden
|-- /admin
|   |-- /dashboard
|   |-- /users
|   |-- /users/import
|   |-- /users/:userId
|   |-- /subjects
|   `-- /subjects/:subjectId
|-- /teacher
|   |-- /dashboard
|   |-- /subjects
|   |-- /subjects/:subjectId/questions
|   |-- /subjects/:subjectId/questions/import
|   |-- /subjects/:subjectId/collections
|   |-- /subjects/:subjectId/collections/:collectionId
|   |-- /exams
|   |-- /exams/new
|   |-- /exams/:examId
|   |-- /exams/:examId/monitor
|   `-- /exams/:examId/results
|-- /student
|   |-- /dashboard
|   |-- /exams
|   |-- /exams/:examId/lobby
|   `-- /results/:resultId
`-- /exam/:sessionId
```

## 5. Design system Sahara

### Mau sac

| Token | Gia tri | Muc dich |
| --- | --- | --- |
| `canvas` | `#faf5ee` | Nen ung dung |
| `surface` | `#fffdf9` | Card, dialog, table |
| `primary` | `#c2652a` | CTA, focus, active |
| `primary-hover` | `#a95120` | Hover CTA |
| `accent` | `#8c3c3c` | Canh bao hoac nhan manh hiem |
| `text` | `#3a302a` | Van ban chinh |
| `muted` | `#756a62` | Mo ta, metadata |
| `border` | `rgba(216,208,200,.6)` | Duong vien |
| `success` | `#55745a` | Da luu, thanh cong |
| `warning` | `#a46b22` | Sap het gio, can chu y |
| `danger` | `#a4443d` | Loi, thao tac nguy hiem |

Khong dung xanh duong lam mau thuong hieu. Mau trang thai phai kem icon hoac
label, khong chi dua vao mau.

### Typography

- Heading: EB Garamond, weight 500-600.
- Body, label, number: Manrope, weight 400-700.
- Desktop H1: 44/48; mobile H1: 34/38.
- Body: 15/24; label: 13/18.
- So lieu dashboard dung Manrope de doc nhanh.

### Khoang cach va hinh dang

- Luoi 8px.
- Card padding desktop 28-32px, mobile 18-20px.
- Radius button/input 8px; card 12px; dialog 16px.
- Shadow: `0 2px 16px rgba(58,48,42,.04)`.
- Content max width 1440px; form max width 720px.

### Component cot loi

- `AppShell`, `Sidebar`, `Topbar`, `Breadcrumb`.
- `PageHeader`, `SectionHeader`, `StatCard`.
- `DataTable`, `MobileCardList`, `Pagination`.
- `FilterBar`, `SearchInput`, `StatusChip`.
- `FormField`, `Select`, `DateTimePicker`, `FileDropzone`.
- `Drawer`, `Dialog`, `ConfirmDialog`.
- `EmptyState`, `ErrorState`, `Skeleton`.
- `Toast`, `InlineAlert`, `ProgressBanner`.
- `QuestionCard`, `QuestionNavigator`, `SaveIndicator`, `ExamTimer`.

## 6. Responsive strategy

### Desktop: tu 1200px

- Sidebar co dinh rong 248px.
- Noi dung table day du, filter tren mot hoac hai hang.
- Detail page co main column va contextual side panel.

### Tablet: 768-1199px

- Sidebar thu gon thanh rail hoac drawer.
- Table an cot phu; action gom vao menu.
- Form hai cot chuyen thanh mot cot neu rong duoi 900px.

### Mobile: duoi 768px

- Navigation drawer.
- Table chuyen thanh card list.
- Filter nang cao nam trong bottom sheet.
- Primary action co the sticky o cuoi man hinh.
- Exam screen mot cot; navigator cuon ngang; timer sticky tren cung.

## 7. Route guard va auth

- Login nhan JWT string tu `/v1/api/auth-service/login`.
- Decode claim `userRole`, `userId`, `fullName`, `displayName`, `exp`.
- Guard route theo `ADMIN`, `TEACHER`, `STUDENT`.
- Request protected gui `Authorization: Bearer <token>`.
- Khi 401: xoa session va dieu huong ve login, giu `returnTo`.
- Khi 403: hien trang khong co quyen, khong gia lap quyen chi bang UI.
- Khong co refresh-token flow hoan chinh: can canh bao truoc khi token 3 gio het
  han trong cac man hinh form dai.

Khuyen nghi luu token trong memory va session storage cho MVP. Khong luu thong
tin nhay cam khac trong local storage.

## 8. Server state va form state

- TanStack Query quan ly request, cache, invalidation va retry.
- React Hook Form + Zod quan ly form va validate phia client.
- Axios instance xu ly base URL, bearer token va response envelope.
- API adapter chuan hoa:
  - Login: plain JWT string.
  - API khac: `{ timestamp, status, message, data }`.
  - Error: `{ timestamp, status, error, message, path }`.
- Zustand chi dung neu exam draft/offline queue vuot qua kha nang local component
  state. Khong them Redux.

## 9. User flows

### Admin

```text
Dang nhap
  -> Tong quan
  -> Tim va loc giao vien/hoc sinh
  -> Xem chi tiet, sua ho so an toan, kich hoat/ngung hoat dong
  -> Import tai khoan Excel
  -> Xem ket qua partial success va loi theo dong
  -> Tao mon hoc
  -> Mo chi tiet mon
  -> Tim giao vien ACTIVE theo ten/ma/email
  -> Chon record that de gan/go giao vien
```

### Teacher: question collection

```text
Dang nhap
  -> Chon mon dang day
  -> Xem ngan hang cau hoi
  -> Import Excel neu can
  -> Tao bo cau hoi
  -> Loc cau chua co trong bo
  -> Chon trang hoac chon tat ca ket qua
  -> Them hang loat
  -> Xem thong ke EASY/MEDIUM/HARD
```

### Teacher: exam lifecycle

```text
Tao ca thi
  -> Chon mon va bo cau hoi
  -> Cau hinh quota, thoi gian, tron cau
  -> Gan hoc sinh
  -> Xem lai
  -> Kich hoat
  -> Giam sat
  -> Dong ca
  -> Xem ket qua va xuat Excel
```

### Student

```text
Dang nhap
  -> Ca thi cua toi
  -> Phong cho
  -> Kiem tra mang/fullscreen
  -> Bat dau thi
  -> Chon dap an + auto-save
  -> Xac nhan nop bai
  -> Trang thai dang cham
  -> Xem ket qua neu duoc cong bo
```

## 10. Danh muc man hinh

### Chung

| Man hinh | Muc tieu |
| --- | --- |
| Dang nhap | Xac thuc, thong bao loi ro rang |
| 403 | Dieu huong ve workspace hop le |
| 404 | Tim lai noi dung hoac ve tong quan |
| Session expired | Giu return URL, yeu cau dang nhap lai |

### Admin

| Man hinh | API |
| --- | --- |
| Tong quan | Real Auth statistics + real Question subject list |
| Danh sach nguoi dung | Real, URL filter/pagination/sort |
| Chi tiet va status nguoi dung | Real, safe profile fields only |
| Import tai khoan | Real multipart partial success |
| Danh sach/tao/sua mon | Real |
| Luu tru/khoi phuc mon | Real |
| Phan cong giao vien | Real Auth search + verified Question mutation |

### Teacher

| Man hinh | API |
| --- | --- |
| Tong quan | Planned aggregate |
| Chon mon | Existing |
| Ngan hang cau hoi | Existing |
| Import cau hoi | Existing |
| Danh sach bo cau hoi | Existing |
| Chi tiet bo cau hoi | Existing |
| Them/xoa cau hoi | Existing |
| Danh sach ca thi | Planned |
| Tao/sua ca thi | Planned |
| Gan hoc sinh | Planned |
| Giam sat thi | Planned |
| Ket qua/thong ke | Planned |

### Student

| Man hinh | API |
| --- | --- |
| Tong quan va ca sap dien ra | Planned |
| Danh sach ca thi | Planned |
| Phong cho | Planned |
| Lam bai | Planned |
| Xac nhan nop bai | Planned |
| Dang cham | Planned |
| Ket qua chi tiet | Planned |

## 11. API map hien tai

Nguon su that: controller trong code, khong phai vi du path cu trong docs.

| Chuc nang | Method va path |
| --- | --- |
| Login | `POST /v1/api/auth-service/login` |
| Register | `POST /v1/api/auth-service/register` |
| Admin user search | `GET /v1/api/admin/auth-service/users` |
| Admin user statistics | `GET /v1/api/admin/auth-service/users/statistics` |
| Admin user detail/update | `GET, PUT /v1/api/admin/auth-service/users/{userId}` |
| Admin user status | `PATCH /v1/api/admin/auth-service/users/{userId}/status` |
| Import user | `POST /v1/api/admin/auth-service/users/import` |
| Admin subject CRUD | `/v1/api/admin/question-service/subjects` |
| Teacher assignment | `/v1/api/admin/question-service/subjects/{subjectId}/teachers` |
| Teacher subjects | `GET /v1/api/question-service/teacher/subjects` |
| Import question | `POST /v1/api/question-service/teacher/subjects/{subjectId}/questions/import` |
| Search question | `GET /v1/api/question-service/teacher/subjects/{subjectId}/questions` |
| Collection CRUD | `/v1/api/question-service/teacher/subjects/{subjectId}/question-collections` |
| Collection items | `.../{collectionId}/questions` |
| Add by filter | `.../{collectionId}/questions/add-by-filter` |

## 12. API can bo sung

### P0 de UX hien tai hoan chinh

- `GET /v1/api/auth-service/me` de lay profile va permission tu server.
- Question detail co options hoac endpoint
  `GET .../questions/{questionId}`.
- Dong nhat error body cho validation va authorization.

Admin user search, statistics, safe profile update, status update, teacher
resolution va Gateway `PATCH` da duoc trien khai. Admin UI khong co input UUID,
mock directory hoac fallback identity.

### P1 cho exam

- Exam CRUD, blueprint, assignments, activate/close.
- Student assigned exams va lobby readiness.
- Runtime start/resume, server time, auto-save, submit.
- Teacher monitor state va proctoring event stream.
- Result list, statistics, detail va Excel export.

## 13. Planned API contract toi thieu

```text
POST /v1/api/exam-service/teacher/exams
GET  /v1/api/exam-service/teacher/exams
GET  /v1/api/exam-service/teacher/exams/{examId}
PUT  /v1/api/exam-service/teacher/exams/{examId}
POST /v1/api/exam-service/teacher/exams/{examId}/assignments
POST /v1/api/exam-service/teacher/exams/{examId}/activate
POST /v1/api/exam-service/teacher/exams/{examId}/close

GET  /v1/api/exam-service/student/exams
POST /v1/api/exam-runtime-service/exams/{examId}/sessions
GET  /v1/api/exam-runtime-service/sessions/{sessionId}
PUT  /v1/api/exam-runtime-service/sessions/{sessionId}/answers
POST /v1/api/exam-runtime-service/sessions/{sessionId}/submit

GET  /v1/api/result-service/teacher/exams/{examId}/results
GET  /v1/api/result-service/teacher/exams/{examId}/statistics
GET  /v1/api/result-service/student/results/{resultId}
```

Submit va auto-save can co `clientSeq`, server timestamp va idempotency key.

## 14. Accessibility

- Contrast dat WCAG AA.
- Focus ring 2px mau primary, khong xoa outline.
- Tat ca form co label va error lien ket bang `aria-describedby`.
- Dialog trap focus va dong bang Escape khi an toan.
- Table co caption; mobile card giu cung thong tin.
- Timer khong announce moi giay; chi announce cac moc 15, 5 va 1 phut.
- Exam navigation co label "Cau da tra loi", "Dang xem", "Chua tra loi".

## 15. Trang thai va feedback

- Loading list: skeleton, khong spinner toan trang.
- Empty state phan biet "chua co du lieu" va "khong co ket qua loc".
- Import hien ten file, kich thuoc, progress va ket qua theo dong.
- Mutation thanh cong cap nhat cache va toast ngan.
- Archive/delete can confirm voi ten resource.
- Bulk action hien so item da chon va ket qua requested/added/skipped.
- Offline exam: banner co dinh, luu local queue, dong bo lai theo thu tu.

## 16. Rui ro va quyet dinh

| Rui ro | Xu ly |
| --- | --- |
| Docs va controller khac path | Sinh API client tu controller/OpenAPI sau nay |
| Auth unavailable khi gan teacher | Question Service fail closed; UI hien loi va refresh search |
| Teacher inactive sau khi search | Backend xac minh lai tai mutation time |
| Assignment cu bi hong | Hien `MISSING`/`NON_TEACHER`, chi cho go |
| Question khong co options | Khong mo man detail day du den khi co contract |
| JWT plain string | Auth adapter rieng |
| Token het han sau 3 gio | Expiry warning, returnTo login |
| Backend exam chua co | MSW mock theo planned contracts |
| Image mockup co the sai text nho | Wireframe va tai lieu la source of truth |

## 17. Thu tu trien khai de xuat

1. Foundation, role guard va shared workspace shell: da trien khai.
2. Admin dashboard, user, import, subject va assignment: da trien khai bang API that.
3. Teacher subject, import question, search va collection: da trien khai.
4. Tiep tuc cac phan Exam/Student/Result con nam ngoai Admin release.
5. Duy tri responsive, accessibility va integration hardening.

## 18. Thanh cong duoc do bang

- Admin hoan thanh import va phan cong ma khong can biet API path.
- Teacher tao bo 250 cau bang filter/bulk action trong it hon 5 phut.
- Student tiep tuc bai sau reload hoac mat mang ngan.
- Moi route tu choi dung role va co recovery path.
- Core flow dung duoc o 360px, 768px va 1440px.
- Khong co thao tac nguy hiem chi dua vao toast de xac nhan.
