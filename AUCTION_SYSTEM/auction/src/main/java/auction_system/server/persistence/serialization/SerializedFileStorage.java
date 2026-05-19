package auction_system.server.persistence.serialization;

import auction_system.server.persistence.exceptions.DatabaseException;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Bộ lưu trữ dữ liệu bằng Java Serialization.
 *
 * <p>Lớp này chỉ chịu trách nhiệm đọc và ghi file .ser. Lớp này không xử lý
 * nghiệp vụ đấu giá, không kiểm tra quyền người dùng và không xử lý Socket.
 *
 * @param <T> kiểu đối tượng cần lưu trữ
 */
public class SerializedFileStorage<T extends Serializable> implements FileStorage<T> {

    /** Đường dẫn file dữ liệu .ser. */
    private final Path filePath;

    /**
     * Khởi tạo bộ lưu trữ serialization.
     *
     * @param filePath đường dẫn file dữ liệu
     */
    public SerializedFileStorage(final Path filePath) {
        this.filePath = Objects.requireNonNull(filePath, "filePath");
    }

    @Override
    public Map<String, T> readAll() {
        if (!Files.exists(filePath)) {
            return new LinkedHashMap<>();
        }

        try (ObjectInputStream inputStream =
            new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(filePath)))) {
            Object object = inputStream.readObject();
            return castToRecordMap(object);
        } catch (EOFException exception) {
            return new LinkedHashMap<>();
        } catch (IOException | ClassNotFoundException exception) {
            throw new DatabaseException(
                "Không thể đọc dữ liệu từ file: " + filePath,
                exception);
        }
    }

    @Override
    public void writeAll(final Map<String, T> records) {
        Objects.requireNonNull(records, "records");

        try {
            Path parentDirectory = filePath.getParent();
            if (parentDirectory != null) {
                Files.createDirectories(parentDirectory);
            }

            Path tempFile = Files.createTempFile(
                parentDirectory,
                "database-",
                ".tmp");

            try (ObjectOutputStream outputStream =
                new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                outputStream.writeObject(new LinkedHashMap<>(records));
            }

            moveTempFileToTarget(tempFile);
        } catch (IOException exception) {
            throw new DatabaseException(
                "Không thể ghi dữ liệu xuống file: " + filePath,
                exception);
        }
    }

    /**
     * Ép kiểu dữ liệu đọc từ file thành bản đồ dữ liệu hợp lệ.
     *
     * @param object đối tượng đọc được từ file
     * @return bản đồ dữ liệu đã được ép kiểu
     */
    @SuppressWarnings("unchecked")
    private Map<String, T> castToRecordMap(final Object object) {
        if (!(object instanceof Map<?, ?> rawMap)) {
            throw new DatabaseException(
                "File dữ liệu không đúng định dạng Map: " + filePath);
        }

        Map<String, T> result = new LinkedHashMap<>();

        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (!(entry.getKey() instanceof String)) {
                throw new DatabaseException(
                    "Khóa dữ liệu không phải String trong file: " + filePath);
            }

            result.put((String) entry.getKey(), (T) entry.getValue());
        }

        return result;
    }

    /**
     * Di chuyển file tạm sang file dữ liệu chính.
     *
     * <p>Ưu tiên atomic move để giảm rủi ro hỏng file khi server đang ghi thì bị
     * dừng đột ngột. Nếu hệ điều hành không hỗ trợ, chuyển sang ghi thay thế.
     *
     * @param tempFile file tạm vừa ghi xong
     * @throws IOException nếu không thể di chuyển file
     */
    private void moveTempFileToTarget(final Path tempFile) throws IOException {
        try {
            Files.move(
                tempFile,
                filePath,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(tempFile, filePath, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}