Bạn đóng vai trò là một Senior Java Developer. Dưới đây là sơ đồ lớp (Class Diagram) của hệ thống "Đấu giá trực tuyến" (Online Auction System). Hãy đọc kỹ cấu trúc này để hiểu ngữ cảnh hệ thống trước khi tôi yêu cầu bạn viết code.

Hệ thống áp dụng OOP chuẩn, kế thừa từ lớp `Entity` và sử dụng 3 Design Patterns: Singleton, Factory Method, Observer.

# 1. ENUMS & INTERFACES
- **Enum AuctionStatus**: OPEN, RUNNING, FINISHED, PAID, CANCELED.
- **Interface AuctionObserver**:
    + Phương thức: `update(message: String): void`
- **Interface ItemCreator** (Factory Method Pattern):
    + Phương thức: `createItem(): Item`

# 2. CORE MODELS (Thực thể dữ liệu)
Tất cả các thực thể đều kế thừa trực tiếp hoặc gián tiếp từ `Entity`.

- **Abstract Class Entity**:
    + Thuộc tính: `#id: String`
    + Phương thức: `+equals(o: Object): boolean`, `+hashCode(): int`

- **Abstract Class User** (extends Entity) (implements AuctionObserver):
    + Thuộc tính: `#username: String`, `#password: String`, `#email: String`, `#isOnline: boolean`
    + Phương thức: `+login(): void`, `+logout(): void`, `+displayDashboard(): void`, `+update(message: String): void`

- **Class Admin** (extends User):
    + Thuộc tính: `-adminRoleLevel: int`
    + Phương thức: `+banUser(user: User): void`, `+removeInvalidItem(item: Item): void`, `+resolveDispute(): void`

- **Abstract Class Item** (extends Entity):
    + Thuộc tính: `#itemName: String`, `#description: String`, `#startingPrice: double`, `#sellerId: String`, `#imagePath: String`
    + Phương thức: `+getDisplayDetails(): void`

- **Class Electronic** (extends Item):
    + Thuộc tính: `-brand: String`, `-warrantyMonths: int`, `-condition: String`

- **Class Art** (extends Item):
    + Thuộc tính: `-artistName: String`, `-creationYear: String`, `-hasAuthenticityCertificate: boolean`

- **Class Vehicle** (extends Item):
    + Thuộc tính: `-make: String`, `-model: String`, `-manufacturingYear: int`, `-mileage: double`

- **Class BidTransaction** (extends Entity):
    + Thuộc tính: `-bidder: Bidder`, `-amount: double`, `-timestamp: LocalDateTime`

- **Class Auction** (extends Entity):
    + Thuộc tính: `-item: Item`, `-seller: Seller`, `-bids: List<BidTransaction>`, `-currentHighestBid: BidTransaction`, `-startTime: LocalDateTime`, `-endTime: LocalDateTime`, `-status: AuctionStatus`, `-observers: List<AuctionObserver>`
    + Phương thức: `+placeBid(bid: BidTransaction): boolean`, `+calculateWinner(): Bidder`, `+attach(observer: AuctionObserver): void`, `+detach(observer: AuctionObserver): void`, `+notifyObservers(): void`

# 3. PATTERNS & MANAGERS

- **Class ElectronicCreator** (implements ItemCreator): `+createItem(): Electronic`
- **Class ArtCreator** (implements ItemCreator): `+createItem(): Art`
- **Class VehicleCreator** (implements ItemCreator): `+createItem(): Vehicle`

- **Class AuctionManager** (Singleton Pattern):
    + Thuộc tính tĩnh: `-instance: AuctionManager`
    + Thuộc tính: `-auctionList: List<Auction>`, `-activeUsers: Map<String, User>`
    + Phương thức: `-AuctionManager()` (private constructor), `+getInstance(): AuctionManager`