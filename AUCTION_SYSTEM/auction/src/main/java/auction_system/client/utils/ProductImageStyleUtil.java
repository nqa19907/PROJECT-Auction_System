package auction_system.client.utils;

import auction_system.client.components.SvgIcon;
import java.io.File;
import java.util.List;
import java.util.Locale;
import javafx.scene.layout.Region;
import javafx.scene.shape.Rectangle;

/**
 * Tiện ích chuyển đường dẫn ảnh sản phẩm thành CSS background cho JavaFX.
 */
public final class ProductImageStyleUtil {
    private static final List<String> CATEGORY_ICON_CLASSES =
            List.of("icon-art", "icon-electronic", "icon-vehicle", "icon-category");

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
     * Hiển thị ảnh thật nếu hợp lệ, nếu không hiển thị icon theo danh mục.
     *
     * @param imageRegion vùng hiển thị ảnh nền
     * @param placeholderIcon icon fallback đặt giữa vùng ảnh
     * @param imagePath đường dẫn ảnh sản phẩm
     * @param category danh mục sản phẩm
     */
    public static void applyImageOrPlaceholder(
            final Region imageRegion,
            final Region placeholderIcon,
            final String imagePath,
            final String category) {
        if (imageRegion == null || placeholderIcon == null) {
            return;
        }

        final String imageStyle = toBackgroundImageStyle(imagePath);
        final boolean showPlaceholder = imageStyle.isBlank();
        imageRegion.setStyle(imageStyle);
        placeholderIcon.setManaged(showPlaceholder);
        placeholderIcon.setVisible(showPlaceholder);

        if (placeholderIcon instanceof SvgIcon svgIcon) {
            svgIcon.setIcon(categoryIconName(category));
        } else {
            placeholderIcon.getStyleClass().removeAll(CATEGORY_ICON_CLASSES);
            placeholderIcon.getStyleClass().add(categoryIconClass(category));
        }
    }

    private static String categoryIconClass(final String category) {
        return "icon-" + categoryIconName(category);
    }

    private static String categoryIconName(final String category) {
        if (category == null) {
            return "category";
        }

        return switch (category.trim().toUpperCase(Locale.ROOT)) {
            case "ART" -> "art";
            case "ELECTRONIC" -> "electronic";
            case "VEHICLE" -> "vehicle";
            default -> "category";
        };
    }

    /**
     * Cắt vùng ảnh theo bo góc thật, bao gồm cả bitmap background.
     *
     * @param imageRegion vùng hiển thị ảnh sản phẩm
     * @param radius bán kính bo góc
     */
    public static void applyRoundedClip(final Region imageRegion, final double radius) {
        if (imageRegion == null) {
            return;
        }

        Rectangle clip = new Rectangle();
        clip.widthProperty().bind(imageRegion.widthProperty());
        clip.heightProperty().bind(imageRegion.heightProperty());
        clip.setArcWidth(radius * 2);
        clip.setArcHeight(radius * 2);
        imageRegion.setClip(clip);
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
