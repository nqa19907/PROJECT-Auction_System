# 🔨 Online Auction System - Bài tập lớn LTNC
[![Build Status](https://github.com/nqa19907/PROJECT-Auction_System/actions/workflows/maven-build.yml/badge.svg)](https://github.com/nqa19907/PROJECT-Auction_System/actions/workflows/maven-build.yml)
![Java Version](https://img.shields.io/badge/Java-21-blue)
![Maven](https://img.shields.io/badge/Build-Maven-orange)

Hệ thống đấu giá trực tuyến xây dựng theo kiến trúc **Client-Server** sử dụng Java thuần, kết nối Socket TCP và giao diện JavaFX.

## 👥 Thành viên nhóm
* **Nguyễn Quốc Anh**
* **Nguyễn Đức Mạnh**
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
## 📂 Cấu trúc mã nguồn (Project Structure)

Dự án được thiết kế theo kiến trúc **Client - Server**, áp dụng **Mô hình MVC** và được quản lý bởi **Maven**. Dưới đây là sơ đồ chi tiết các phân hệ cốt lõi:

```text
auction
├── config
│   └── custom_checks.xml                            # CẤU HÌNH CHUẨN GOOGLE CHECKSTYLE
├── pom.xml                                          # CẤU HÌNH MAVEN - QUẢN LÝ THƯ VIỆN
├── src
│   ├── main
│   │   ├── java
│   │   │   └── auction_system
│   │   │       ├── client                           # TẦNG CLIENT - GIAO DIỆN & TƯƠNG TÁC UI
│   │   │       │   ├── ClientApp.java               # Khởi động ứng dụng JavaFX
│   │   │       │   ├── controllers                  # Xử lý sự kiện (Dashboard, Login, Register...)
│   │   │       │   ├── network
│   │   │       │   │   └── ServerConnection.java    # Mở kết nối Socket lên Server
│   │   │       │   └── ...                          # (Các dịch vụ và session bổ trợ)
│   │   │       │
│   │   │       ├── common                           # TẦNG COMMON - LOGIC DÙNG CHUNG 2 BÊN
│   │   │       │   ├── exceptions                   # Các class báo lỗi (AuctionClosed, InvalidBid...)
│   │   │       │   ├── models                       # Các mô hình đối tượng cốt lõi
│   │   │       │   │   ├── auctions                 # Thực thể đấu giá (Auction, BidTransaction...)
│   │   │       │   │   ├── items                    # Sản phẩm (Art, Electronic, Vehicle...)
│   │   │       │   │   │   ├── builder              # Áp dụng Builder Pattern cho kế thừa
│   │   │       │   │   │   └── factory              # Áp dụng Factory Pattern khởi tạo Item
│   │   │       │   │   └── users                    # Phân quyền (Admin, Bidder, Seller, User)
│   │   │       │   └── network
│   │   │       │       └── Protocol.java            # Quy tắc giao tiếp (Từ điển lệnh)
│   │   │       │
│   │   │       └── server                           # TẦNG SERVER - XỬ LÝ NGHIỆP VỤ BACKEND
│   │   │           ├── Launcher.java                # Lớp bọc mồi khởi chạy ứng dụng
│   │   │           ├── ServerApp.java               # Điểm khởi chạy hệ thống Server
│   │   │           ├── core
│   │   │           │   └── AuctionManager.java      # Bộ não xử lý logic và quản lý đa luồng
│   │   │           └── network
│   │   │               ├── SocketServer.java        # Cổng chờ kết nối chính (TCP)
│   │   │               ├── ClientHandler.java       # Xử lý luồng riêng cho mỗi Client
│   │   │               └── command                  # Áp dụng Command Pattern xử lý yêu cầu
│   │   │
│   │   └── resources
│   │       └── client
│   │           ├── css                              # File trang trí giao diện
│   │           ├── fxml                             # Bản vẽ giao diện (Login, Sign, Dashboard...)
│   │           └── images                           # Kho chứa Icon, Logo và Banner
│   │
│   └── test                                         # THƯ MỤC KIỂM THỬ (UNIT TEST)
│       └── java
│           ├── CoreBiddingLogicTest.java            # Kiểm tra logic đặt giá tranh chấp
│           ├── AuctionLifecycleTest.java            # Kiểm tra vòng đời phiên đấu giá
│           └── ...                                  # (Các bài test Factory và App)
│
└── target                                           # THƯ MỤC BUILD (TỰ ĐỘNG SINH - BỎ QUA)
```