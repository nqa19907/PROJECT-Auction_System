package auction_system.server.persistence;

import java.nio.file.Path;
import java.util.Objects;

/**
 * Cấu hình vị trí lưu trữ dữ liệu serialization của server.
 *
 * <p>Lớp này giúp tránh hard-code đường dẫn trong repository, đồng thời giúp
 * việc đổi thư mục dữ liệu dễ hơn khi chạy trên máy khác hoặc trên CI/CD.
 */
public class SerializedDatabaseConfig {
  /** Thư mục gốc chứa toàn bộ file dữ liệu của server. */
  private final Path dataDirectory;

  /**
   * Khởi tạo cấu hình lưu trữ dữ liệu.
   *
   * @param dataDirectory thư mục chứa các file .ser
   */
  public SerializedDatabaseConfig(final Path dataDirectory) {
    this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
  }

  /**
   * Trả về đường dẫn file lưu người dùng.
   *
   * @return đường dẫn file users.ser
   */
  public Path usersFile() {
    return dataDirectory.resolve("users.ser");
  }

  /**
   * Trả về đường dẫn file lưu sản phẩm.
   *
   * @return đường dẫn file items.ser
   */
  public Path itemsFile() {
    return dataDirectory.resolve("items.ser");
  }

  /**
   * Trả về đường dẫn file lưu phiên đấu giá.
   *
   * @return đường dẫn file auctions.ser
   */
  public Path auctionsFile() {
    return dataDirectory.resolve("auctions.ser");
  }

  /**
   * Trả về đường dẫn file lưu lịch sử đặt giá.
   *
   * @return đường dẫn file bid_transactions.ser
   */
  public Path bidTransactionsFile() {
    return dataDirectory.resolve("bid_transactions.ser");
  }
}