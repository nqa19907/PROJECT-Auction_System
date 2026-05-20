package auction_system.server.persistence.repositories;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

/**
 * Định nghĩa các thao tác chung của kho dữ liệu trong hệ thống.
 *
 * <p>Repository đóng vai trò như một tập hợp đối tượng domain. Service sẽ gọi
 * repository thay vì tự đọc hoặc ghi file trực tiếp.
 *
 * @param <T> kiểu đối tượng domain cần quản lý
 */
public interface Repository<T extends Serializable> {

    /**
     * Lưu hoặc cập nhật một đối tượng.
     *
     * @param entity đối tượng cần lưu
     * @return đối tượng đã lưu
     */
    T save(T entity);

    /**
     * Tìm đối tượng theo mã định danh.
     *
     * @param id mã định danh đối tượng
     * @return đối tượng nếu tồn tại
     */
    Optional<T> findById(String id);

    /**
     * Lấy toàn bộ đối tượng đang được quản lý.
     *
     * @return danh sách đối tượng
     */
    List<T> findAll();

    /**
     * Kiểm tra đối tượng có tồn tại hay không.
     *
     * @param id mã định danh đối tượng
     * @return true nếu tồn tại, false nếu không tồn tại
     */
    boolean existsById(String id);

    /**
     * Xóa đối tượng theo mã định danh.
     *
     * @param id mã định danh đối tượng
     * @return true nếu có xóa, false nếu không tìm thấy
     */
    boolean deleteById(String id);

    /**
     * Tải lại dữ liệu từ file.
     *
     * <p>Hàm này chỉ nên dùng khi server cần đồng bộ lại dữ liệu từ bộ nhớ ngoài.
     */
    void reload();

    /**
     * Ghi dữ liệu hiện tại xuống file.
     */
    void flush();
}