# MoneyKeep - Ứng Dụng Quản Lý Tài Chính Cá Nhân

## Giới thiệu

MoneyKeep là ứng dụng quản lý tài chính cá nhân được phát triển trên nền tảng Android nhằm hỗ trợ người dùng theo dõi các khoản thu nhập, chi tiêu và quản lý nhiều ví tiền khác nhau một cách hiệu quả.

Ứng dụng giúp người dùng kiểm soát dòng tiền, theo dõi số dư hiện tại của từng ví, quản lý lịch sử giao dịch và xây dựng kế hoạch chi tiêu hợp lý.

## Mục tiêu đề tài

* Xây dựng ứng dụng quản lý tài chính cá nhân trên thiết bị Android.
* Hỗ trợ quản lý các khoản thu nhập và chi tiêu hằng ngày.
* Quản lý nhiều ví tiền trong cùng một ứng dụng.
* Theo dõi số dư và lịch sử giao dịch.
* Hỗ trợ cảnh báo khi số dư ví xuống dưới mức giới hạn do người dùng thiết lập.
* Vận dụng kiến thức về Android, Room Database và mô hình MVVM vào thực tế.

---

## Công nghệ sử dụng

### Ngôn ngữ lập trình

* Kotlin

### Kiến trúc phần mềm

* MVVM (Model - View - ViewModel)

### Giao diện

* Jetpack Compose
* Material Design 3

### Cơ sở dữ liệu

* Room Database
* SQLite

### Thư viện hỗ trợ

* Kotlin Coroutines
* StateFlow / Flow
* Navigation Compose
* ViewModel
* Room Persistence Library

---

## Chức năng chính

### 1. Quản lý ví

* Thêm ví mới.
* Chỉnh sửa thông tin ví.
* Thiết lập mức cảnh báo số dư cho từng ví.
* Theo dõi số dư hiện tại của từng ví.
* Gộp nhiều ví thành một ví duy nhất.
* Tự động cập nhật các giao dịch liên quan sau khi gộp ví.

### 2. Quản lý giao dịch

* Thêm giao dịch thu nhập.
* Thêm giao dịch chi tiêu.
* Chỉnh sửa giao dịch.
* Xóa giao dịch.
* Lưu lịch sử giao dịch.
* Phân loại giao dịch theo danh mục.

### 3. Thống kê tài chính

* Hiển thị tổng số dư của tất cả ví.
* Tổng hợp thu nhập và chi tiêu.
* Theo dõi tình hình tài chính cá nhân.
* Hỗ trợ người dùng đánh giá thói quen chi tiêu.

### 4. Cảnh báo ngân sách

* Thiết lập mức cảnh báo số dư.
* Thông báo khi số dư ví thấp hơn ngưỡng đã đặt.
* Hỗ trợ kiểm soát chi tiêu hiệu quả.

### 5. Sao lưu và khôi phục dữ liệu

* Xuất dữ liệu để sao lưu.
* Nhập dữ liệu từ bản sao lưu.
* Hạn chế mất dữ liệu khi thay đổi thiết bị hoặc cài đặt lại ứng dụng.

---

## Cấu trúc dự án

```text
com.example.moneykeep
│
├── data
│   ├── local
│   └── repository
├── navigation
│   └── AppNavigation.kt
│
├── ui
│   ├── components
│   ├── screens
│   └── theme
│
├── viewmodel
│
└── MainActivity.kt
```
## Điều hướng ứng dụng (Navigation Compose)

Ứng dụng sử dụng Jetpack Navigation Compose để quản lý việc chuyển đổi giữa các màn hình.

Toàn bộ cấu hình điều hướng được xây dựng trong file `navigation/AppNavigation.kt`. Thành phần `NavHost` được sử dụng để khai báo các route và quản lý màn hình hiện tại thông qua `NavController`.

Các route chính của ứng dụng:

| Route           | Chức năng                |
| --------------- | ------------------------ |
| welcome         | Màn hình chào mừng       |
| home            | Trang chủ                |
| wallet          | Quản lý ví               |
| report          | Thống kê                 |
| more            | Cài đặt và tiện ích khác |
| add_transaction | Thêm giao dịch           |
| create_wallet   | Tạo ví mới               |

Luồng điều hướng:

```text
Welcome
   ↓
Home
├── Wallet
│     └── Create Wallet
├── Report
├── More
└── Add Transaction
```

Việc sử dụng Navigation Compose giúp quản lý điều hướng tập trung, dễ mở rộng và phù hợp với kiến trúc MVVM kết hợp Jetpack Compose.

```
```
---

## Yêu cầu hệ thống

* Android 8.0 (API 26) trở lên.
* Android Studio.
* Thiết bị Android hoặc Android Emulator.

---

## Hướng dẫn cài đặt

### Bước 1: Clone dự án

```bash
git clone <repository-url>
```

### Bước 2: Mở dự án bằng Android Studio

* Chọn Open.
* Chọn thư mục dự án MoneyKeep.

### Bước 3: Đồng bộ Gradle

Chờ Android Studio tải và cài đặt các thư viện cần thiết.

### Bước 4: Chạy ứng dụng

* Kết nối thiết bị Android hoặc mở Android Emulator.
* Nhấn Run để khởi chạy ứng dụng.

---

## Kết quả đạt được

* Xây dựng thành công ứng dụng quản lý tài chính cá nhân trên Android.
* Áp dụng mô hình MVVM giúp dễ bảo trì và mở rộng hệ thống.
* Sử dụng Room Database để lưu trữ dữ liệu cục bộ ổn định.
* Hỗ trợ quản lý ví và giao dịch hiệu quả.
* Thực hiện được chức năng gộp ví và cập nhật dữ liệu liên quan.
* Hỗ trợ cảnh báo số dư giúp người dùng kiểm soát tài chính tốt hơn.

---

## Hạn chế

* Chưa hỗ trợ đăng nhập và đồng bộ dữ liệu trực tuyến.
* Chưa hỗ trợ chia sẻ dữ liệu giữa nhiều thiết bị.
* Chưa tích hợp hệ thống thông báo tự động theo lịch.

---

## Hướng phát triển

* Tích hợp Firebase để đồng bộ dữ liệu trực tuyến.
* Bổ sung chức năng đăng nhập tài khoản.
* Xây dựng hệ thống biểu đồ thống kê trực quan.
* Tích hợp nhắc nhở và thông báo thông minh.
* Hỗ trợ đa ngôn ngữ.
* Ứng dụng AI để phân tích và đề xuất kế hoạch chi tiêu.
* **Tích hợp API kết nối với các ngân hàng** để tự động đọc và phân tích 
  thông báo SMS/email biến động số dư, từ đó tự động ghi nhận giao dịch 
  vào ứng dụng MoneyKeep mà không cần nhập tay.
---

## Tác giả

Đề tài được thực hiện bởi nhóm 3 sinh viên trường **Đại học Hạ Long** trong quá trình 
học tập môn Lập trình Android nhằm vận dụng kiến thức về phát triển ứng dụng di động, 
cơ sở dữ liệu và kiến trúc phần mềm vào việc xây dựng một ứng dụng quản lý tài chính 
cá nhân thực tế.
---

## Giấy phép

Dự án được phát triển phục vụ mục đích học tập và nghiên cứu.
