# Online Auction System - Bài tập lớn Lập trình nâng cao

## 1. Mô tả ngắn gọn bài toán và phạm vi hệ thống

Online Auction System là hệ thống đấu giá trực tuyến được xây dựng theo mô hình
client-server. Hệ thống cho phép người dùng đăng ký, đăng nhập, đăng sản phẩm,
tham gia phiên đấu giá và đặt giá theo thời gian thực.

Phạm vi hệ thống gồm:
- Quản lý người dùng và phân quyền.
- Quản lý sản phẩm/phiên đấu giá.
- Tham gia đấu giá và cập nhật người dẫn đầu.
- Kết thúc phiên đấu giá, xác định người thắng.
- Xử lý lỗi nghiệp vụ và lỗi kết nối.
- Giao diện JavaFX cho client.
- Các chức năng nâng cao: realtime update, auto-bidding, anti-sniping,
  bid history visualization, nạp tiền và lọc sản phẩm theo loại.

Các vai trò chính:
- Admin: quản lý người dùng và phiên đấu giá.
- Seller/Participant: đăng sản phẩm, quản lý phiên của mình.
- Bidder/Participant: xem phiên đấu giá, đặt giá, theo dõi lịch sử bid.

## 2. Công nghệ sử dụng, môi trường chạy và yêu cầu cài đặt

### Công nghệ sử dụng

- Ngôn ngữ: Java 21.
- Giao diện: JavaFX 21, FXML, CSS.
- Networking: Socket TCP, xử lý đa luồng cho nhiều client.
- Kiến trúc: Client-Server, MVC.
- Lưu trữ dữ liệu: Serialization qua file trong thư mục data.
- Build tool: Maven.
- Kiểm thử: JUnit 5.
- Logging: SLF4J, Logback.
- Coding convention: Google Java Style, Maven Checkstyle.
- CI/CD: GitHub Actions.

### Design Pattern chính

- Command Pattern: xử lý các request từ client gửi lên server.
- Observer Pattern: cập nhật realtime khi có bid mới.
- Factory Method: tạo các loại Item như Art, Electronic, Vehicle.
- Builder Pattern: hỗ trợ khởi tạo model có nhiều thuộc tính.
- Singleton/Registry: quản lý tập trung auction, user online và lifecycle.

### Yêu cầu môi trường

- JDK 21 trở lên.
- Maven 3.8+ hoặc Maven Wrapper có sẵn trong dự án.
- Hệ điều hành hỗ trợ JavaFX runtime.
- Port server mặc định: 8080.
- Client mặc định kết nối tới host: 127.0.0.1.

## 3. Cấu trúc thư mục và các module chính

```text
Auction_Project
├── AUCTION_SYSTEM                         
│   └── auction                            
│       ├── config                         # Cấu hình kiểm tra coding convention
│       │   └── custom_checks.xml          # Rule Checkstyle tùy chỉnh cho dự án
│       ├── pom.xml                        # Khai báo dependency, plugin build, test và đóng gói JAR
│       ├── src                            
│       │   ├── main                      
│       │   │   ├── java                  
│       │   │   │   └── auction_system     
│       │   │   │       ├── client         
│       │   │   │       │   ├── controllers # Controller MVC cho các màn hình FXML
│       │   │   │       │   ├── models     # ViewModel/context phục vụ hiển thị dữ liệu
│       │   │   │       │   ├── network    # NetworkClient và DTO nhận phản hồi từ server
│       │   │   │       │   ├── security   # Chính sách hiển thị theo vai trò người dùng
│       │   │   │       │   ├── services   # Service client gọi lệnh socket và parse response
│       │   │   │       │   ├── session    # Quản lý phiên đăng nhập phía client
│       │   │   │       │   └── utils      # Tiện ích định dạng, router, xử lý ảnh và scene
│       │   │   │       ├── common         
│       │   │   │       │   ├── constants  # Hằng số nghiệp vụ và cấu hình chung
│       │   │   │       │   ├── exceptions # Exception nghiệp vụ như bid không hợp lệ, phiên đã đóng
│       │   │   │       │   ├── models     # Entity/model đấu giá, item, user, bid transaction
│       │   │   │       │   ├── network    # Protocol JSON và cấu hình giao tiếp socket
│       │   │   │       │   └── utils      # Tiện ích chung như bảo mật và hash mật khẩu
│       │   │   │       └── server         
│       │   │   │           ├── core       # Logic lõi: lifecycle, registry, anti-sniping, settlement
│       │   │   │           ├── network    # Socket server, client handler và command dispatcher
│       │   │   │           ├── persistence # Repository và serialization database
│       │   │   │           ├── services   # Service nghiệp vụ cho auth, auction, bidding, auto-bid
│       │   │   │           └── session    # Theo dõi session client đang kết nối server
│       │   │   └── resources              
│       │   │       └── client             # Resource dành cho JavaFX client
│       │   │           ├── css            # Stylesheet giao diện
│       │   │           ├── fxml           # Layout màn hình JavaFX
│       │   │           ├── fonts          # Font nhúng trong giao diện
│       │   │           └── images         # Ảnh, icon và banner sản phẩm
│       │   └── test                       # Unit test JUnit cho logic quan trọng
│       │       └── java                   # Test source code
│       └── target                         # Output sau khi Maven build/package
├── data                                   # Dữ liệu runtime lưu bằng serialization
├── docs                                   # Tài liệu bổ sung nếu nhóm thêm sau
├── .github                                # Cấu hình GitHub cho repository
│   └── workflows                          # GitHub Actions build/test tự động
└── README.md                              
```

### Vai trò các module chính

- `auction_system.client`: ứng dụng JavaFX phía client, gồm controller, service,
  session, network client và các tiện ích hiển thị.
- `auction_system.common`: model, exception, protocol và hằng số dùng chung giữa
  client và server.
- `auction_system.server`: xử lý nghiệp vụ, socket server, command, repository,
  persistence, realtime notification và lifecycle auction.
- `src/main/resources/client/fxml`: các màn hình giao diện JavaFX.
- `src/main/resources/client/css`: stylesheet cho giao diện.
- `src/test/java`: unit test cho bidding, lifecycle, repository, factory,
  observer, authentication và các logic quan trọng.
- `data`: dữ liệu runtime được lưu bằng serialization.
- `.github/workflows`: cấu hình GitHub Actions cho build/test tự động.

## 4. Vị trí các file .jar

Sau khi build, các file JAR nằm tại:

```text
AUCTION_SYSTEM/auction/target/
├── auction-1.4-server.jar
├── auction-1.4-client.jar
└── auction-1.4.jar
```

File nên dùng khi chạy:
- Server: `AUCTION_SYSTEM/auction/target/auction-1.4-server.jar`
- Client: `AUCTION_SYSTEM/auction/target/auction-1.4-client.jar`

Nếu thư mục `target` chưa có file JAR, cần build lại bằng Maven.

## 5. Hướng dẫn chạy Server/Client theo thứ tự cụ thể

### Bước 1: Mở terminal tại thư mục Maven project

```bash
cd AUCTION_SYSTEM/auction
```

### Bước 2: Build và chạy test

```bash
mvn clean test
```

### Bước 3: Đóng gói JAR

```bash
mvn clean package
```

Sau bước này, kiểm tra các file `.jar` trong:

```text
AUCTION_SYSTEM/auction/target/
```

### Bước 4: Chạy server trước

Mở terminal thứ nhất:

```bash
java -jar target/auction-1.4-server.jar
```

Server sẽ mở Socket TCP tại:

```text
Host: 127.0.0.1
Port: 8080
```

### Bước 5: Chạy client sau khi server đã sẵn sàng

Mở terminal thứ hai:

```bash
java -jar target/auction-1.4-client.jar
```

Có thể mở nhiều client cùng lúc để kiểm thử realtime bidding và concurrent
bidding.

### Bước 6: Quy trình dùng thử cơ bản

1. Chạy server.
2. Chạy một hoặc nhiều client.
3. Đăng ký hoặc đăng nhập tài khoản.
4. Tạo sản phẩm/phiên đấu giá bằng tài khoản có quyền đăng bán.
5. Dùng client khác tham gia phiên đấu giá.
6. Đặt giá cao hơn giá hiện tại.
7. Quan sát realtime update, bid history và trạng thái phiên đấu giá.
8. Kiểm tra kết quả khi phiên kết thúc.

## 6. Danh sách chức năng đã hoàn thành

### Chức năng bắt buộc

- Đăng ký tài khoản.
- Đăng nhập tài khoản.
- Phân quyền người dùng: Admin và Participant/Seller/Bidder.
- Admin quản lý người dùng.
- Admin quản lý hoặc hủy/xóa phiên đấu giá.
- Người bán đăng sản phẩm đấu giá.
- Người bán quản lý phiên đấu giá của mình.
- Danh sách phiên đấu giá.
- Chi tiết phiên đấu giá.
- Người dùng tham gia phiên đấu giá.
- Đặt giá hợp lệ cao hơn giá hiện tại.
- Cập nhật người dẫn đầu phiên đấu giá.
- Lưu lịch sử giao dịch đặt giá.
- Tự động xử lý vòng đời phiên đấu giá.
- Xác định người thắng khi phiên kết thúc.
- Xử lý lỗi đặt giá thấp hơn giá hiện tại.
- Xử lý lỗi đặt giá khi phiên chưa chạy hoặc đã đóng.
- Xử lý lỗi dữ liệu và lỗi kết nối ở client/server.
- Giao diện JavaFX với FXML/CSS.

### Chức năng nâng cao

- Auto-Bidding: người dùng đặt maxBid và increment để hệ thống tự trả giá.
- Anti-sniping: tự gia hạn phiên nếu có bid mới ở thời điểm cuối.
- Realtime Update: thông báo bid mới qua Observer/Socket, không dùng polling liên tục.
- Bid History Visualization: biểu đồ đường thể hiện lịch sử giá realtime.

## 7. Link báo cáo PDF và video demo

- báo cáo PDF: BaoCaoDuAn.pdf trong folder gốc
- Link video demo: https://youtu.be/eMDtJN-CxMA
