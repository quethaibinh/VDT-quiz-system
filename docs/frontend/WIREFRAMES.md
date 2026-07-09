# Quiz Platform Wireframes

Tai lieu nay la source of truth cho layout va noi dung. Mockup bitmap chi minh
hoa visual direction.

## 1. App shell

```text
+----------------------+------------------------------------------------------+
| SAHARA QUIZ          | Breadcrumb                         Search   User      |
|                      +------------------------------------------------------+
| Navigation           | Page title                              Primary CTA |
| - Tong quan          | Supporting description                             |
| - ...                +------------------------------------------------------+
|                      |                                                      |
|                      | Main content                                         |
|                      |                                                      |
+----------------------+------------------------------------------------------+
```

Mobile: topbar co menu, logo va avatar. Navigation mo bang drawer.

## 2. Dang nhap

```text
+--------------------------------------------------------------------------+
| SAHARA QUIZ                                                             |
|                                                                          |
|  Khong gian thi cu gon gang      +------------------------------------+  |
|  cho thay va tro.                | Chao mung tro lai                 |  |
|                                  | Ten dang nhap [______________]    |  |
|  "Hoc tap can su tap trung,      | Mat khau       [______________]   |  |
|   cong cu nen that tinh."        | [ ] Ghi nho trong phien           |  |
|                                  | [       Dang nhap       ]         |  |
|                                  | Loi dang nhap neu co              |  |
|                                  +------------------------------------+  |
+--------------------------------------------------------------------------+
```

## 3. Admin dashboard

```text
Tong quan
[Nguoi dung ACTIVE] [Giao vien ACTIVE] [Hoc sinh ACTIVE]
[Mon dang hoat dong] [Mon da luu tru]

Thao tac nhanh
[Quan ly nguoi dung] [Import tai khoan] [Quan ly mon hoc]
```

Ba card nguoi dung dung `GET .../users/statistics`. Hai card mon hoc duoc tinh
tu danh sach subject that. Hai section load/error/retry doc lap; khong co hoat
dong gan day, viec can xu ly hoac so lieu gia lap.

## 4. Import tai khoan

```text
Import tai khoan

+--------------------------------------------------------------------------+
| [Chon tep Excel .xlsx]                                                   |
| student-import-100.xlsx | 24.5 KB               [Bat dau import]          |
+--------------------------------------------------------------------------+

Ket qua: 98 thanh cong | 2 that bai
Dong | Truong       | Ma loi                    | Mo ta
  4  | studentCode  | DUPLICATE_STUDENT_CODE    | Ma da ton tai
```

Mobile: bang loi thanh card theo dong.

## 5. Admin subject list

```text
Mon hoc                                             [Tao mon hoc]
[Tim theo ma hoac ten................] [Trang thai v]

[PHI101] Triet hoc dai cuong       Hoat dong       [Chi tiet & phan cong]
[MAT101] Toan cao cap              Luu tru         [Chi tiet & phan cong]
```

Tao/sua mon dung drawer ben phai, gom code, name va description. Danh sach la
card responsive va chi hien du lieu Question Service tra ve.

## 6. Subject detail va teacher assignment

```text
< Mon hoc
Triet hoc dai cuong                         [Sua] [Luu tru]
PHI101 | Hoat dong

Thong tin mon             Giao vien dang day
Mo ta...                  [Tim ten/ma/email................]

                          Nguyen Van An  GV001 ACTIVE     [Phan cong]

Phan cong hien tai
Nguyen Van An     GV001   Dang hoat dong    [Go phan cong]
Tran Minh Chau    GV014   Ngung hoat dong   [Go phan cong]
Khong nhan dien   <real stored id> MISSING [Go phan cong]
```

Teacher picker chi tim `TEACHER` co status `ACTIVE` qua Auth Admin API. Admin
chon record theo ten/ma; khong co truong nhap UUID. Teacher da duoc gan bi
disable. Subject da archive an picker nhung van hien assignment de go.

## 7. Teacher subject chooser

```text
Mon hoc cua toi
Chon mot mon de quan ly cau hoi, bo cau hoi va ca thi.

[Triet hoc dai cuong]       [Kinh te chinh tri]
 PHI101                      ECO102
 480 cau hoi                 260 cau hoi (planned aggregate)
 [Mo khong gian mon]         [Mo khong gian mon]
```

## 8. Teacher dashboard trong subject

```text
Triet hoc dai cuong                                 [Doi mon]
[480 cau hoi] [8 bo cau hoi] [3 ca thi sap toi] [2 can xu ly]

Thao tac nhanh
[Import cau hoi] [Tao bo cau hoi] [Tao ca thi]

Bo cau hoi gan day                 Ca thi sap toi
...
```

## 9. Import cau hoi

```text
Import cau hoi / Triet hoc dai cuong

[Tai file mau]  Quy tac: validate toan file, co loi thi khong luu dong nao.

+--------------------------------------------------------------------------+
| Keo file .xlsx vao day hoac [Chon file]                                  |
+--------------------------------------------------------------------------+

Ket qua thanh cong
100 dong | 100 cau da tao | 6 chu de moi

Ket qua loi
Dong | Truong          | Loi                         | Huong sua
  7  | correctOptions  | INVALID_CORRECT_OPTION      | Dap an C dang rong
```

## 10. Question bank

```text
Ngan hang cau hoi                                  [Import Excel]
[Tim noi dung...] [Chu de v] [Do kho v] [Hien thi v] [So huu v] [Bo loc]

[ ]  Noi dung                         Chu de       Do kho   Hien thi   ...
[ ]  Theo Marx, vat chat...           Chuong 1     Vua      Cong khai
[ ]  Menh de nao dung...              Chuong 2     Kho      Rieng tu

Da chon 12                         [Them vao bo cau hoi]
```

Question row desktop mo quick preview. Mobile dung card, checkbox o goc tren.

## 11. Collection list

```text
Bo cau hoi                                         [Tao bo cau hoi]
[Tim...] [So huu: Tat ca v] [Hien thi v] [Trang thai v]

+-----------------------------------+ +-----------------------------------+
| Bo cau hoi giua ky               | | On tap chuong 1                  |
| Rieng tu | Co the chinh sua      | | Cong khai | Chi xem              |
| 250 cau                           | | 80 cau                            |
| 120 De | 90 Vua | 40 Kho         | | 30 De | 35 Vua | 15 Kho          |
| Cap nhat 10 phut truoc [Mo bo]   | | Giao vien B              [Mo bo]  |
+-----------------------------------+ +-----------------------------------+
```

## 12. Collection detail

```text
< Bo cau hoi
Bo cau hoi giua ky                 Rieng tu        [Sua] [Luu tru]
250 cau | 120 De | 90 Vua | 40 Kho

[Cau hoi trong bo] [Them cau hoi]

[Tim...] [Chu de v] [Do kho v] [Membership: Trong bo v]
[ ] Cau hoi ...                                      [Xoa khoi bo]

Da chon 8                                      [Xoa 8 cau khoi bo]
```

Tab Them cau hoi dung `membership=NOT_IN`; co hai action:

- Them cac cau da chon.
- Them tat ca ket qua bo loc, mo confirm neu matched lon.

## 13. Exam list

```text
Ca thi                                                [Tao ca thi]
[Tim...] [Trang thai v] [Khoang ngay v]

Giua ky Triet hoc       14/06 08:00    Cho bat dau    120 hoc sinh [Mo]
Kiem tra chuong 1       12/06 14:00    Ban nhap       40 hoc sinh  [Mo]
```

Planned API.

## 14. Exam builder

```text
Tao ca thi
1 Thong tin -- 2 Cau hoi -- 3 Hoc sinh -- 4 Cau hinh -- 5 Xem lai

Ten ca thi         [________________________________________]
Mon hoc            [Triet hoc dai cuong v]
Bo cau hoi         [Bo cau hoi giua ky v]
Bat dau            [14/06/2026] [08:00]
Thoi luong         [60] phut

                                               [Luu nhap] [Tiep tuc]
```

Buoc Cau hoi hien quota va canh bao neu collection khong du cau:

```text
De  [20] / 120   Vua [20] / 90   Kho [10] / 40   Tong 50
```

## 15. Exam monitor

```text
Giua ky Triet hoc             Dang dien ra  00:42:18       [Dong ca]
[120 tham gia] [112 online] [5 canh bao] [3 da nop]

[Tat ca] [Canh bao] [Mat ket noi]          [Tim hoc sinh...]

Hoc sinh       Trang thai     Tien do      Vi pham        Lan cuoi
Nguyen Van A   Online         32/50        0               vua xong
Tran Thi B     Mat ket noi    21/50        Thoat full 2    40 giay

Event stream
14:18 Tran Thi B thoat fullscreen lan 2
```

Desktop co event panel ben phai; mobile chia tab "Hoc sinh" va "Su kien".

## 16. Teacher results

```text
Ket qua / Giua ky Triet hoc                         [Xuat Excel]
[Da cham 118/120] [Trung binh 7.2] [Cao nhat 9.8] [Thap nhat 2.5]

Phan bo diem (chart)             Cau sai nhieu nhat

[Tim hoc sinh...] [Trang thai v]
Hoc sinh        Diem   Dung/Sai   Nop luc       Trang thai
Nguyen Van A    8.5    43/7       09:01         Da cham
```

## 17. Student dashboard va exam list

```text
Chao buoi sang, Minh

Ca thi sap toi
+--------------------------------------------------------------------------+
| Giua ky Triet hoc              Bat dau sau 1 gio 18 phut                 |
| 08:00, 14/06/2026 | 60 phut                         [Vao phong cho]       |
+--------------------------------------------------------------------------+

Ca thi khac
[Sap dien ra] [Da ket thuc]
```

## 18. Exam lobby

```text
Giua ky Triet hoc
Bat dau luc 08:00 | 60 phut | 50 cau

Kiem tra truoc khi vao thi
[OK] Ket noi mang
[OK] Kich thuoc man hinh
[--] Che do toan man hinh                 [Bat toan man hinh]

Luu y:
- Khong roi khoi tab thi.
- Bai duoc tu dong luu.
- Dong ho duoc tinh theo may chu.

                                      [Bat dau lam bai]
```

Nut bat dau chi active khi server cho phep va checklist dat yeu cau.

## 19. Exam focus mode

```text
+--------------------------------------------------------------------------+
| Giua ky Triet hoc      Da luu 10:32       Con 42:18      [Nop bai]       |
+--------------------------------------------------------------------------+
| Cau 8 / 50                                             [Danh sach cau]  |
|                                                                          |
| Theo quan diem...                                                        |
|                                                                          |
| ( ) A. ...                                                               |
| ( ) B. ...                                                               |
| ( ) C. ...                                                               |
| ( ) D. ...                                                               |
|                                                                          |
| [Cau truoc]                                      [Danh dau] [Cau sau]    |
+--------------------------------------------------------------------------+
| 1  2  3  4  5  6  7 [8] 9 10 ... 50                                    |
+--------------------------------------------------------------------------+
```

Trang thai navigator: chua tra loi, da tra loi, dang xem, danh dau.

Mobile:

```text
+--------------------------------------+
| Con 42:18        Da luu       [Nop]  |
| Cau 8/50                            |
| Noi dung...                         |
| ( ) A...                            |
| ( ) B...                            |
|                                    |
| [Truoc]                    [Sau]    |
| 1 2 3 4 5 [8] 9 ...                |
+--------------------------------------+
```

## 20. Submit confirmation

```text
Nop bai?

Ban da tra loi 46/50 cau.
4 cau chua tra loi: 12, 27, 41, 50.

[Quay lai kiem tra]                    [Xac nhan nop bai]
```

Sau submit 202:

```text
Da ghi nhan bai nop
Ma bai nop: ...
He thong dang cham bai. Ban co the dong trang nay.
```

## 21. Student result

```text
Ket qua Giua ky Triet hoc
8.5 / 10                 43 dung | 7 sai | 58 phut

[Tong quan] [Chi tiet cau hoi]

Neu giao vien chua cong bo chi tiet:
"Ket qua chi tiet se duoc cong bo sau."
```

Khong hien correct answer neu policy chua cho phep.

## 22. Trang thai loi quan trong

### Mat ket noi trong luc thi

```text
Dang mat ket noi. Cau tra loi van duoc luu tren thiet bi.
He thong se tu dong dong bo khi ket noi tro lai.
```

### Session het han

```text
Phien dang nhap da het han.
[Dang nhap lai] de quay lai trang dang mo.
```

### Collection khong editable

```text
Bo cau hoi nay duoc chia se boi giao vien khac.
Ban co the xem va su dung, nhung khong the chinh sua.
```
