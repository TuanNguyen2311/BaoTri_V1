# Bảo Trì Thiết Bị - Android App

Ứng dụng quản lý bảo trì thiết bị nhà máy/xưởng. Chạy **offline hoàn toàn**, không cần internet.

---

## Kiến trúc

```
Clean Architecture + MVVM + Jetpack Compose
```

```
app/
├── data/
│   ├── db/          # Room Database, DAOs, Converters
│   ├── model/       # Room Entities
│   └── repository/  # Repository implementations + Mappers
├── domain/
│   ├── model/       # Domain models (pure Kotlin)
│   ├── repository/  # Repository interfaces
│   └── usecase/     # Business logic UseCases
├── ui/
│   ├── auth/        # Login, ChangePassword, SetupPIN, ForgotPassword
│   ├── ktv/         # Dashboard, Scan, Detail, WriteLog, History
│   ├── manager/     # Dashboard, Device, Report, Account, Backup, Settings
│   └── shared/      # Components, Theme
├── util/            # SecurityUtil, QRUtil, DateUtil, SessionManager
└── di/              # Hilt DI Module
```

---

## Tech Stack

| Layer | Tech |
|-------|------|
| UI | Jetpack Compose + Material3 |
| Navigation | Navigation Compose |
| DI | Hilt |
| Database | Room (SQLite) |
| State | ViewModel + StateFlow |
| Camera/QR Scan | CameraX + ML Kit Barcode |
| QR Generate | ZXing |
| Image Loading | Coil |
| Encryption | AES-256 (backup), SHA-256 (password/PIN) |
| Preferences | DataStore |

---

## Cách build

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 17
- Android SDK 34

### Bước 1: Mở project
```bash
# Clone hoặc copy thư mục BaoTriThietBi vào máy
# Mở Android Studio → Open → chọn thư mục BaoTriThietBi
```

### Bước 2: Sync Gradle
```
Android Studio tự động sync. Nếu không:
File → Sync Project with Gradle Files
```

### Bước 3: Build APK
```
Build → Build Bundle(s) / APK(s) → Build APK(s)
APK nằm tại: app/build/outputs/apk/debug/app-debug.apk
```

### Bước 4: Cài lên thiết bị Android
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

---

## Tài khoản mặc định

Khi cài app lần đầu, hệ thống tự tạo:

| Trường | Giá trị |
|--------|---------|
| Username | `admin` |
| Password | `1234` |
| Vai trò | Quản lý |

> **Lưu ý:** App sẽ bắt đổi mật khẩu và tạo PIN ngay lần đăng nhập đầu tiên.

---

## Tính năng chính

### Kỹ thuật viên
- Đăng nhập bằng tài khoản cá nhân
- Scan QR / Barcode thiết bị bằng camera
- Xem thông tin và lịch sử bảo trì thiết bị
- Ghi maintenance log (mô tả lỗi, cách xử lý, đính kèm ảnh)
- Xem lịch sử log cá nhân

### Quản lý
- Dashboard tổng quan: thống kê, biểu đồ, cảnh báo
- Quản lý thiết bị: thêm/sửa/xóa, sinh QR, in PDF
- Báo cáo/thống kê theo tuần/tháng/quý/năm
- Quản lý tài khoản KTV: thêm/vô hiệu hóa/reset mật khẩu
- Backup & Khôi phục dữ liệu (mã hóa AES-256, định dạng .btdb)

---

## Bảo mật

- Mật khẩu lưu dạng SHA-256 hash
- PIN khẩn cấp lưu dạng SHA-256 hash  
- File backup mã hóa AES-256 + kiểm tra checksum toàn vẹn
- Giới hạn 3 lần thử PIN sai

---

## Ghi chú cho developer

### Thêm màn hình mới
1. Thêm route vào `Navigation.kt`
2. Tạo ViewModel với `@HiltViewModel`
3. Tạo Composable Screen
4. Đăng ký trong `AppNavGraph.kt`

### Thêm UseCase mới
1. Thêm interface method vào `domain/repository/`
2. Implement trong `data/repository/`
3. Tạo UseCase class trong `domain/usecase/`
4. Inject vào ViewModel qua Hilt

### Database migration
Tăng `version` trong `AppDatabase.kt` và thêm Migration object.

---

**Phiên bản:** 1.0.0  
**Target:** Android 8.0+ (API 26+)  
**Ngôn ngữ:** Kotlin  
