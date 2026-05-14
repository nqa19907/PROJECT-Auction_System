package auction_system.common.models.items.builder;

/**
 * Giao diện chung cho tất cả các lớp Builder trong hệ thống.
 *
 * @param <T> Kiểu dữ liệu của đối tượng sẽ được tạo ra.
 */
public interface Builder<T> {
    T build();
}