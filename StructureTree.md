# Cấu Trúc Dự Án (Auction System)

Dưới đây là cây cấu trúc cải thiện của dự án, phản ánh đúng các mẫu thiết kế (Design Patterns) và logic nghiệp vụ đang được sử dụng trong codebase hiện tại:

```text
AUCTION_SYSTEM/
└── auction/
    ├── .gitignore
    ├── pom.xml
    ├── README.md
    └── src/
        ├── main/
        │   ├── java/
        │   │   └── auction_system/
        │   │       ├── client/                # CLIENT CODE (Giao diện JavaFX & Kết nối Server)
        │   │       │   ├── controllers/       # Các Controller điều khiển giao diện (LoginController, DashboardController...)
        │   │       │   ├── patterns/
        │   │       │   │   └── singleton/     # Quản lý phiên bản cục bộ và Socket (SessionManager, ServerConnection)
        │   │       │   └── ClientApp.java     # Lớp khởi chạy ứng dụng phía Client
        │   │       ├── common/                # SHARED CODE (Dùng chung cho cả Client và Server)
        │   │       │   ├── enums/             # Định nghĩa các trạng thái (AuctionStatus)
        │   │       │   ├── exceptions/        # Các ngoại lệ (Exceptions) tùy chỉnh
        │   │       │   ├── models/            # Các lớp thực thể chính
        │   │       │   │   ├── auctions/      # Thông tin phiên đấu giá (Auction, BidTransaction, Entity)
        │   │       │   │   ├── constants/     # Các hằng số (Protocol - Giao thức Socket)
        │   │       │   │   ├── items/         # Các loại mặt hàng (Art, Electronic, Vehicle, Item)
        │   │       │   │   └── users/         # Phân quyền người dùng (Admin, Bidder, Seller, Participant, User)
        │   │       │   └── patterns/          # Design Patterns dùng chung
        │   │       │       ├── builder/       # Builder Pattern (Tạo đối tượng phức tạp như ArtBuilder...)
        │   │       │       ├── factory/       # Factory Method Pattern (Tạo thể hiện của Item: ItemCreator...)
        │   │       │       └── observer/      # Observer Pattern (Lắng nghe cập nhật từ đấu giá: AuctionObserver)
        │   │       └── server/                # SERVER CODE (Xử lý Socket, DB, Logic đấu giá)
        │   │           ├── network/           # Xử lý I/O luồng kết nối TCP (ClientHandler)
        │   │           ├── patterns/
        │   │           │   ├── command/       # Command Pattern (Điều hướng lệnh từ Socket: LoginCommand, PlaceBidCommand...)
        │   │           │   └── singleton/     # Chứa Server core (SocketServer, AuctionManager, DbConnection)
        │   │           ├── session/           # Quản lý phiên làm việc của từng User trên Server (ClientSession)
        │   │           ├── Launcher.java      # Wrapper khởi động JavaFX 11+
        │   │           └── ServerApp.java     # Lớp khởi chạy Server (Có thể kèm UI quản trị)
        │   └── resources/
        │       └── client/                    # Tệp tĩnh (Static resources)
        │           ├── css/                   # Stylesheet giao diện
        │           ├── fxml/                  # Các layout JavaFX
        │           └── images/                # Hình ảnh, logo, banner...
        └── test/
            └── java/                          # Các Unit Test (JUnit 5)
                ├── AuctionLifecycleTest.java
                ├── CoreBiddingLogicTest.java
                └── ItemFactoryTest.java
```