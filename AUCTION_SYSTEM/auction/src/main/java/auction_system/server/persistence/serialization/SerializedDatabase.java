package auction_system.server.persistence.serialization;

import auction_system.server.persistence.repositories.AuctionRepository;
import auction_system.server.persistence.repositories.AutoBidSettingRepository;
import auction_system.server.persistence.repositories.BidTransactionRepository;
import auction_system.server.persistence.repositories.ItemRepository;
import auction_system.server.persistence.repositories.UserRepository;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * Điểm truy cập tập trung tới toàn bộ repository của hệ thống.
 *
 * <p>Lớp này đóng vai trò như database context cho server. Service có thể lấy
 * repository từ đây thay vì tự khởi tạo từng repository riêng lẻ.
 */
public class SerializedDatabase {

    /** Repository quản lý người dùng. */
    private final UserRepository userRepository;

    /** Repository quản lý sản phẩm đấu giá. */
    private final ItemRepository itemRepository;

    /** Repository quản lý phiên đấu giá. */
    private final AuctionRepository auctionRepository;

    /** Repository quản lý lịch sử giao dịch đặt giá. */
    private final BidTransactionRepository bidTransactionRepository;

    /** Repository quản lý cấu hình auto-bid. */
    private final AutoBidSettingRepository autoBidSettingRepository;

    /** Khóa giao dịch ở mức database để tránh nhiều client ghi chéo dữ liệu. */
    private final ReentrantLock transactionLock;

    /**
     * Khởi tạo database serialization từ thư mục dữ liệu.
     *
     * @param dataDirectory thư mục chứa các file .ser
     */
    public SerializedDatabase(final Path dataDirectory) {
        this(new SerializedDatabaseConfig(dataDirectory));
    }

    /**
     * Khởi tạo database serialization từ cấu hình có sẵn.
     *
     * @param config cấu hình đường dẫn dữ liệu
     */
    public SerializedDatabase(final SerializedDatabaseConfig config) {
        Objects.requireNonNull(config, "config");

        this.userRepository =
            new UserRepository(config.usersFile());

        this.itemRepository =
            new ItemRepository(config.itemsFile());

        this.auctionRepository =
            new AuctionRepository(config.auctionsFile());

        this.bidTransactionRepository =
            new BidTransactionRepository(
                config.bidTransactionsFile()
            );

        this.autoBidSettingRepository =
            new AutoBidSettingRepository(config.autoBidSettingsFile());

        this.transactionLock = new ReentrantLock();
    }

    /**
     * Trả về repository người dùng.
     *
     * @return repository người dùng
     */
    public UserRepository users() {
        return userRepository;
    }

    /**
     * Trả về repository sản phẩm.
     *
     * @return repository sản phẩm
     */
    public ItemRepository items() {
        return itemRepository;
    }

    /**
     * Trả về repository phiên đấu giá.
     *
     * @return repository phiên đấu giá
     */
    public AuctionRepository auctions() {
        return auctionRepository;
    }

    /**
     * Trả về repository lịch sử đặt giá.
     *
     * @return repository lịch sử đặt giá
     */
    public BidTransactionRepository bidTransactions() {
        return bidTransactionRepository;
    }

    /**
     * Trả về repository cấu hình auto-bid.
     *
     * @return repository cấu hình auto-bid
     */
    public AutoBidSettingRepository autoBidSettings() {
        return autoBidSettingRepository;
    }

    /**
     * Chạy một khối xử lý trong khóa giao dịch của database.
     *
     * <p>Nên dùng hàm này cho các thao tác cần cập nhật nhiều repository cùng
     * lúc, ví dụ: đặt giá thành công thì vừa cập nhật Auction, vừa lưu
     * BidTransaction.
     *
     * @param action khối xử lý cần chạy
     * @param <T> kiểu dữ liệu trả về
     * @return kết quả của khối xử lý
     */
    public <T> T executeInTransaction(final Supplier<T> action) {
        Objects.requireNonNull(action, "action");

        transactionLock.lock();

        try {
            return action.get();
        } finally {
            transactionLock.unlock();
        }
    }

    /**
     * Ghi toàn bộ dữ liệu hiện tại xuống các file .ser.
     */
    public void flushAll() {
        transactionLock.lock();

        try {
            userRepository.flush();
            itemRepository.flush();
            auctionRepository.flush();
            bidTransactionRepository.flush();
            autoBidSettingRepository.flush();
        } finally {
            transactionLock.unlock();
        }
    }

    /**
     * Tải lại toàn bộ dữ liệu từ các file .ser.
     */
    public void reloadAll() {
        transactionLock.lock();

        try {
            userRepository.reload();
            itemRepository.reload();
            auctionRepository.reload();
            bidTransactionRepository.reload();
            autoBidSettingRepository.reload();
        } finally {
            transactionLock.unlock();
        }
    }
}
