# 🔨 Online Auction System - Bài tập lớn LTNC
[![Build Status](https://github.com/nqa19907/PROJECT-Auction_System/actions/workflows/maven-build.yml/badge.svg)](https://github.com/nqa19907/PROJECT-Auction_System/actions/workflows/maven-build.yml)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Build-Maven-orange)

Hệ thống đấu giá trực tuyến xây dựng theo kiến trúc **Client-Server** sử dụng Java thuần, kết nối Socket TCP và giao diện JavaFX.

## 👥 Thành viên nhóm
* **Nguyễn Quốc Anh** * **Nguyễn Đức Mạnh**
* **Nguyễn Trọng Hoàng**
* **Bùi Nguyễn Phương**

---

## 🏗️ Kiến trúc & Công nghệ sử dụng
Dự án được thiết kế chuẩn mô hình **MVC (Model-View-Controller)** và áp dụng các kỹ thuật lập trình nâng cao:
* **Networking:** Socket TCP (Đa luồng - Multithreading).
* **Database:** Serialization (Lưu trữ trạng thái hệ thống qua file).
* **Testing:** JUnit 5 (Đảm bảo logic đặt giá và concurrency).
* **CI/CD:** GitHub Actions & Checkstyle (Google Java Style Guide).

### 🎨 Design Patterns áp dụng
- **Command Pattern:** Xử lý điều hướng các yêu cầu từ Client gửi lên Server.
- **Observer Pattern:** Cập nhật giá thầu thời gian thực (Real-time update) cho toàn bộ Client.
- **Factory Method:** Khởi tạo linh hoạt các loại mặt hàng (Art, Electronic, Vehicle).
- **Singleton:** Quản lý tập trung `AuctionManager` và các kết nối cơ sở dữ liệu.
- **Builder Pattern:** Khởi tạo các đối tượng thực thể (Models) phức tạp.

---

## 📂 Cấu trúc mã nguồn
Dự án được tổ chức theo chuẩn Maven, tách biệt rõ ràng giữa logic dùng chung, mã máy chủ và mã máy khách.

```text
AUCTION_SYSTEM/
└── auction/
    ├── src/
    │   ├── main/
    │   │   ├── java/
    │   │   │   └── auction_system/
    │   │   │       ├── client/             # CLIENT SIDE (JavaFX & Networking)
    │   │   │       │   ├── controllers/    # Xử lý logic giao diện (Login, Dashboard)
    │   │   │       │   ├── patterns/       # Singleton quản lý Socket cục bộ
    │   │   │       │   └── ClientApp.java  # Khởi chạy ứng dụng Client
    │   │   │       ├── common/             # SHARED CORE (Dùng chung Client/Server)
    │   │   │       │   ├── enums/          # Trạng thái phiên (OPEN, RUNNING, FINISHED)
    │   │   │       │   ├── models/         # Thực thể OOP (User, Item, Auction, Bid)
    │   │   │       │   └── patterns/       # Triển khai Builder, Factory, Observer
    │   │   │       └── server/             # SERVER SIDE (Logic & Concurrency)
    │   │   │           ├── network/        # Xử lý kết nối TCP (ClientHandler)
    │   │   │           ├── patterns/       # Điều phối lệnh bằng Command Pattern
    │   │   │           ├── session/        # Quản lý phiên làm việc của User
    │   │   │           └── ServerApp.java  # Khởi chạy Socket Server
    │   │   └── resources/              # FXML, CSS và Hình ảnh
    │   └── test/                       # Unit Test cho logic đấu giá và đa luồng
    ├── pom.xml                         # Cấu hình Maven dependencies
    └── .gitignore                      # Loại bỏ file rác IDE (.idea, target)
```