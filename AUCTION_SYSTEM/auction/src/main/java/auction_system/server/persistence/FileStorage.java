package auction_system.server.persistence;

import java.io.Serializable;
import java.util.Map;

/**
 * Định nghĩa hành vi đọc và ghi dữ liệu xuống bộ nhớ ngoài.
 *
 * <p>Interface này giúp repository không phụ thuộc trực tiếp vào cách lưu file.
 * Sau này nếu đổi từ serialization sang JSON, CSV hoặc SQL thì chỉ cần viết
 * implementation mới cho interface này.
 *
 * @param <T> kiểu đối tượng cần lưu trữ
 */
public interface FileStorage<T extends Serializable> {

    /**
     * Đọc toàn bộ dữ liệu từ bộ nhớ ngoài.
     *
     * @return bản đồ dữ liệu với khóa là mã định danh đối tượng
     */
    Map<String, T> readAll();

    /**
     * Ghi toàn bộ dữ liệu xuống bộ nhớ ngoài.
     *
     * @param records bản đồ dữ liệu cần lưu
     */
    void writeAll(Map<String, T> records);
}