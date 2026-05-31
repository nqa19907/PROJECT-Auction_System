import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.client.services.ProductImageStorage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử lưu ảnh sản phẩm phía client.
 */
class ProductImageStorageTest {
    @TempDir
    private Path tempDir;

    @Test
    void storeImageCopiesSupportedImageIntoTargetDirectory() throws IOException {
        Path sourceImage = tempDir.resolve("source.png");
        Files.write(sourceImage, new byte[] {1, 2, 3});
        ProductImageStorage storage = new ProductImageStorage(tempDir.resolve("stored"));

        // Copy ảnh hợp lệ vào thư mục đích và giữ đúng nội dung file.
        Path storedImage = storage.storeImage(sourceImage);

        assertTrue(Files.exists(storedImage));
        assertTrue(storedImage.getFileName().toString().endsWith(".png"));
        assertEquals(3L, Files.size(storedImage));
    }

    @Test
    void storeImageRejectsUnsupportedExtension() throws IOException {
        Path sourceFile = tempDir.resolve("source.txt");
        Files.write(sourceFile, new byte[] {1});
        ProductImageStorage storage = new ProductImageStorage(tempDir.resolve("stored"));

        // Từ chối file không thuộc nhóm định dạng ảnh được hỗ trợ.
        assertThrows(IllegalArgumentException.class, () -> storage.storeImage(sourceFile));
    }

    @Test
    void storeImageRejectsMissingFile() {
        ProductImageStorage storage = new ProductImageStorage(tempDir.resolve("stored"));

        // Từ chối đường dẫn không trỏ tới file ảnh thật.
        assertThrows(
                IllegalArgumentException.class,
                () -> storage.storeImage(tempDir.resolve("missing.png")));
    }
}
