package auction_system.server.persistence.exceptions;

/**
 * Ngoại lệ đại diện cho lỗi xảy ra trong tầng lưu trữ dữ liệu của hệ thống.
 *
 * <p>Lớp này được dùng để bọc các lỗi đọc file, ghi file, lỗi dữ liệu bị hỏng
 * hoặc lỗi khi khởi tạo kho dữ liệu serialization.
 */
public class DatabaseException extends RuntimeException {

    /** Mã định danh phiên bản tuần tự hóa. */
    private static final long serialVersionUID = 1L;

    /**
     * Khởi tạo ngoại lệ lưu trữ với thông điệp lỗi.
     *
     * @param message thông điệp mô tả lỗi
     */
    public DatabaseException(final String message) {
        super(message);
    }

    /**
     * Khởi tạo ngoại lệ lưu trữ với thông điệp lỗi và nguyên nhân gốc.
     *
     * @param message thông điệp mô tả lỗi
     * @param cause nguyên nhân gốc gây ra lỗi
     */
    public DatabaseException(
        final String message,
        final Throwable cause
    ) {
        super(message, cause);
    }
}