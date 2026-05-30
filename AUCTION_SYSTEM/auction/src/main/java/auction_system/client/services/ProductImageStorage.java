package auction_system.client.services;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Lưu ảnh sản phẩm do client chọn vào thư mục dữ liệu của ứng dụng.
 */
public final class ProductImageStorage {
    private static final ProductImageStorage INSTANCE = new ProductImageStorage(defaultDirectory());
    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif");

    private final Path imageDirectory;

    /**
     * Khởi tạo bộ lưu ảnh với thư mục đích cụ thể.
     *
     * @param imageDirectory thư mục lưu ảnh sản phẩm
     */
    public ProductImageStorage(final Path imageDirectory) {
        this.imageDirectory = Objects.requireNonNull(imageDirectory, "imageDirectory")
                .toAbsolutePath()
                .normalize();
    }

    /**
     * Lấy instance dùng chung cho client.
     *
     * @return bộ lưu ảnh mặc định
     */
    public static ProductImageStorage getInstance() {
        return INSTANCE;
    }

    /**
     * Copy ảnh nguồn vào thư mục ảnh sản phẩm của ứng dụng.
     *
     * @param sourceImage ảnh người dùng đã chọn
     * @return đường dẫn tuyệt đối tới ảnh đã lưu
     * @throws IOException nếu không đọc hoặc copy được ảnh
     */
    public Path storeImage(final Path sourceImage) throws IOException {
        Path normalizedSource = validateSourceImage(sourceImage);
        String extension = extractExtension(normalizedSource);
        Path targetImage = imageDirectory.resolve(UUID.randomUUID() + "." + extension);

        // Lưu ảnh vào thư mục dữ liệu ổn định để không phụ thuộc file gốc.
        Files.createDirectories(imageDirectory);
        Files.copy(normalizedSource, targetImage, StandardCopyOption.REPLACE_EXISTING);
        return targetImage.toAbsolutePath().normalize();
    }

    /**
     * Kiểm tra ảnh nguồn trước khi copy vào dữ liệu ứng dụng.
     *
     * @param sourceImage ảnh người dùng đã chọn
     * @return đường dẫn ảnh đã chuẩn hóa
     * @throws IOException nếu không đọc được thông tin file
     */
    private Path validateSourceImage(final Path sourceImage) throws IOException {
        if (sourceImage == null) {
            throw new IllegalArgumentException("Vui lòng chọn ảnh sản phẩm.");
        }

        Path normalizedSource = sourceImage.toAbsolutePath().normalize();
        if (!Files.isRegularFile(normalizedSource)) {
            throw new IllegalArgumentException("File ảnh sản phẩm không tồn tại.");
        }

        // Giới hạn dung lượng ảnh để tránh copy file quá lớn vào dữ liệu app.
        long fileSize = Files.size(normalizedSource);
        if (fileSize <= 0 || fileSize > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException(
                    "Ảnh sản phẩm phải có dung lượng từ 1 byte đến 5 MB.");
        }

        String extension = extractExtension(normalizedSource);
        if (!SUPPORTED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("Ảnh sản phẩm chỉ hỗ trợ JPG, JPEG, PNG hoặc GIF.");
        }

        return normalizedSource;
    }

    /**
     * Lấy phần mở rộng file ảnh ở dạng chữ thường.
     *
     * @param imagePath đường dẫn ảnh
     * @return phần mở rộng file
     */
    private String extractExtension(final Path imagePath) {
        String fileName = imagePath.getFileName().toString();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new IllegalArgumentException("File ảnh sản phẩm phải có phần mở rộng hợp lệ.");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    /**
     * Xác định thư mục ảnh mặc định theo thư mục chạy ứng dụng.
     *
     * @return thư mục lưu ảnh mặc định
     */
    private static Path defaultDirectory() {
        return Path.of(System.getProperty("user.dir"), "data", "item-images");
    }
}
