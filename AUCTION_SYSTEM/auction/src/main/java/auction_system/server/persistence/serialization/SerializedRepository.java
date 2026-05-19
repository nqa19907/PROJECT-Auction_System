package auction_system.server.persistence.serialization;

import auction_system.server.persistence.exceptions.DatabaseException;
import auction_system.server.persistence.repositories.Repository;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Repository tổng quát dùng Java Serialization để lưu trữ dữ liệu.
 *
 * <p>Lớp này dùng khóa đọc/ghi để an toàn hơn khi server xử lý nhiều client
 * đồng thời. Các thao tác ghi như save, delete, reload, flush đều được bảo vệ
 * bằng write lock.
 *
 * @param <T> kiểu đối tượng cần lưu trữ
 */
public class SerializedRepository<T extends Serializable> implements Repository<T> {

    /** Bộ lưu trữ file phía dưới repository. */
    private final FileStorage<T> storage;

    /** Hàm lấy mã định danh từ đối tượng domain. */
    private final Function<T, String> idExtractor;

    /** Bộ nhớ đệm dữ liệu hiện tại của repository. */
    private final Map<String, T> records;

    /** Khóa đọc ghi để tránh lỗi khi nhiều client thao tác cùng lúc. */
    private final ReentrantReadWriteLock lock;

    /**
     * Khởi tạo repository serialization.
     *
     * @param storage bộ lưu trữ file
     * @param idExtractor hàm lấy mã định danh của đối tượng
     */
    public SerializedRepository(
        final FileStorage<T> storage,
        final Function<T, String> idExtractor) {
        this.storage = Objects.requireNonNull(storage, "storage");
        this.idExtractor = Objects.requireNonNull(idExtractor, "idExtractor");
        this.records = new LinkedHashMap<>(storage.readAll());
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public T save(final T entity) {
        Objects.requireNonNull(entity, "entity");

        String id = extractValidId(entity);

        lock.writeLock().lock();
        try {
            records.put(id, entity);
            storage.writeAll(records);
            return entity;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<T> findById(final String id) {
        validateId(id);

        lock.readLock().lock();
        try {
            return Optional.ofNullable(records.get(id));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<T> findAll() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(records.values());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean existsById(final String id) {
        validateId(id);

        lock.readLock().lock();
        try {
            return records.containsKey(id);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public boolean deleteById(final String id) {
        validateId(id);

        lock.writeLock().lock();
        try {
            T removed = records.remove(id);
            if (removed == null) {
                return false;
            }

            storage.writeAll(records);
            return true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void reload() {
        lock.writeLock().lock();
        try {
            records.clear();
            records.putAll(storage.readAll());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void flush() {
        lock.writeLock().lock();
        try {
            storage.writeAll(records);
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Lấy và kiểm tra mã định danh của đối tượng.
     *
     * @param entity đối tượng cần lấy mã
     * @return mã định danh hợp lệ
     */
    private String extractValidId(final T entity) {
        String id = idExtractor.apply(entity);
        validateId(id);
        return id;
    }

    /**
     * Kiểm tra mã định danh có hợp lệ hay không.
     *
     * @param id mã định danh cần kiểm tra
     */
    private void validateId(final String id) {
        if (id == null || id.isBlank()) {
            throw new DatabaseException("Mã định danh dữ liệu không được rỗng.");
        }
    }
}