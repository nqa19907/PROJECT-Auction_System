package auction_system.client.utils;

import auction_system.client.components.SvgIcon;
import javafx.scene.control.CheckBox;

/**
 * Gắn graphic SVG giữ đúng tỉ lệ cho checkbox JavaFX.
 */
public final class CheckBoxIconUtil {
    private static final String CHECKED_ICON = "check-box-checked";
    private static final String UNCHECKED_ICON = "check-box-unchecked";

    private CheckBoxIconUtil() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * Dùng SvgIcon thay cho box/mark mặc định và đồng bộ icon theo trạng thái chọn.
     *
     * @param checkBox checkbox cần áp dụng icon
     */
    public static void apply(final CheckBox checkBox) {
        if (checkBox == null) {
            return;
        }

        final SvgIcon icon = new SvgIcon();
        icon.getStyleClass().add("checkbox-svg-icon");
        updateIcon(icon, checkBox.isSelected());
        checkBox.setGraphic(icon);
        checkBox.selectedProperty().addListener(
                (observable, oldValue, selected) -> updateIcon(icon, selected));
    }

    private static void updateIcon(final SvgIcon icon, final boolean selected) {
        icon.setIcon(selected ? CHECKED_ICON : UNCHECKED_ICON);
    }
}
