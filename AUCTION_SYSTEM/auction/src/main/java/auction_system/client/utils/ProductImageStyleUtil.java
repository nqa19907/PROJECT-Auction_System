package auction_system.client.utils;

import java.io.File;

/**
 * Tiện ích chuyển đường dẫn ảnh sản phẩm thành CSS background cho JavaFX.
 */
public final class ProductImageStyleUtil {
    private ProductImageStyleUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Tạo CSS background image từ đường dẫn ảnh sản phẩm.
     *
     * @param imagePath đường dẫn ảnh sản phẩm
     * @return CSS inline hoặc chuỗi rỗng nếu ảnh không hợp lệ
     */
    public static String toBackgroundImageStyle(final String imagePath) {
        String imageUrl = resolveImageUrl(imagePath);
        if (imageUrl.isBlank()) {
            return "";
        }

        // Escape URL trước khi đưa vào CSS inline của JavaFX.
        return "-fx-background-image: url('" + escapeCssUrl(imageUrl) + "');";
    }

    /**
     * Chuẩn hóa đường dẫn ảnh thành URL JavaFX CSS dùng được.
     *
     * @param imagePath đường dẫn ảnh sản phẩm
     * @return URL ảnh hoặc chuỗi rỗng nếu không hợp lệ
     */
    private static String resolveImageUrl(final String imagePath) {
        if (imagePath == null || imagePath.trim().isEmpty()) {
            return "";
        }

        String trimmedPath = imagePath.trim();
        if (trimmedPath.startsWith("file:")
                || trimmedPath.startsWith("http:")
                || trimmedPath.startsWith("https:")) {
            return trimmedPath;
        }

        File imageFile = new File(trimmedPath);
        if (!imageFile.exists()) {
            return "";
        }
        return imageFile.toURI().toString();
    }

    /**
     * Escape ký tự đặc biệt trước khi đưa URL vào CSS inline.
     *
     * @param value URL ảnh cần escape
     * @return URL đã escape
     */
    private static String escapeCssUrl(final String value) {
        return value.replace("\\", "\\\\").replace("'", "\\'");
    }
}
