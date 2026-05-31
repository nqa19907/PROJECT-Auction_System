package auction_system.client.components;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.geometry.Bounds;
import javafx.scene.layout.Region;
import javafx.scene.shape.FillRule;
import javafx.scene.shape.SVGPath;

/**
 * SVG icon that keeps the original path aspect ratio while fitting its region.
 */
public final class SvgIcon extends Region {

    private static final String CATEGORY_PATH =
            "m260-520 220-360 220 360H260ZM700-80q-75 0-127.5-52.5T520-260q0-75 "
                    + "52.5-127.5T700-440q75 0 127.5 52.5T880-260q0 75-52.5 "
                    + "127.5T700-80Zm-580-20v-320h320v320H120Zm580-60q42 0 "
                    + "71-29t29-71q0-42-29-71t-71-29q-42 0-71 29t-29 71q0 42 "
                    + "29 71t71 29Zm-500-20h160v-160H200v160Zm202-420h156l-78-126-78 "
                    + "126Zm78 0ZM360-340Zm340 80Z";
    private static final String ART_PATH =
            "M160-80q-33 0-56.5-23.5T80-160v-480q0-33 23.5-56.5T160-720h160"
                    + "l160-160 160 160h160q33 0 56.5 23.5T880-640v480q0 33-23.5 "
                    + "56.5T800-80H160Zm0-80h640v-480H160v480Zm80-80h480L570-440 "
                    + "450-280l-90-120-120 160Zm502.5-217.5Q760-475 760-500t-17.5-42.5"
                    + "Q725-560 700-560t-42.5 17.5Q640-525 640-500t17.5 42.5Q675-440 "
                    + "700-440t42.5-17.5ZM404-720h152l-76-76-76 76ZM160-160v-480 480Z";
    private static final String ELECTRONIC_PATH =
            "M400-560ZM160-160q-33 0-56.5-23.5T80-240v-480q0-33 23.5-56.5T160-800"
                    + "h640q33 0 56.5 23.5T880-720H160v480h80v80h-80Zm640-80v-320H640"
                    + "v320h160Zm-180 80q-25 0-42.5-17.5T560-220v-360q0-25 17.5-42.5"
                    + "T620-640h200q25 0 42.5 17.5T880-580v360q0 25-17.5 42.5"
                    + "T820-160H620Zm100-300q13 0 21.5-9t8.5-21q0-13-8.5-21.5T720-520"
                    + "q-12 0-21 8.5t-9 21.5q0 12 9 21t21 9ZM340-160l-20-70q-19-17"
                    + "-29.5-40T280-320q0-27 10.5-50t29.5-40l20-70h120l20 70q19 17 "
                    + "29.5 40t10.5 50q0 27-10.5 50T480-230l-20 70H340Zm60-100q26 0 "
                    + "43-17.5t17-42.5q0-25-18-42.5T400-380q-24 0-42 17t-18 43q0 26 "
                    + "17 43t43 17Zm320-140Z";
    private static final String VEHICLE_PATH =
            "M200-160q-85 0-142.5-57.5T0-360q0-85 58.5-142.5T200-560q77 0 "
                    + "129.5 46T396-400h26l-72-200h-70v-80h200v80h-44l14 40h192"
                    + "l-58-160H480v-80h104q26 0 46.5 14t29.5 38l68 186h32q83 0 "
                    + "141.5 58.5T960-362q0 84-58 143t-142 59q-72 0-126.5-45T564-320"
                    + "H396q-14 69-68 114.5T200-160Zm0-80q41 0 70.5-22.5T312-320H200"
                    + "v-80h112q-12-36-41.5-58T200-480q-51 0-85.5 34.5T80-360q0 50 "
                    + "34.5 85t85.5 35Zm308-160h56q5-23 13.5-43t22.5-37H478l30 80"
                    + "Zm252 160q51 0 85.5-35t34.5-85q0-51-34.5-85.5T760-480h-4"
                    + "l40 106-76 28-38-106q-20 17-31 40t-11 52q0 50 34.5 85t85.5 "
                    + "35ZM196-360Zm564 0Z";
    private static final String ADD_PATH =
            "M440-120v-320H120v-80h320v-320h80v320h320v80H520v320h-80Z";
    private static final String FORMAT_LIST_BULLETED_PATH =
            "M360-200v-80h480v80H360Zm0-240v-80h480v80H360Zm0-240v-80h480v80"
                    + "H360ZM200-160q-33 0-56.5-23.5T120-240q0-33 23.5-56.5T200-320q33 "
                    + "0 56.5 23.5T280-240q0 33-23.5 56.5T200-160Zm0-240q-33 0-56.5-23.5"
                    + "T120-480q0-33 23.5-56.5T200-560q33 0 56.5 23.5T280-480q0 33 "
                    + "-23.5 56.5T200-400Zm-56.5-263.5Q120-687 120-720t23.5-56.5Q167-800 "
                    + "200-800t56.5 23.5Q280-753 280-720t-23.5 56.5Q233-640 200-640"
                    + "t-56.5-23.5Z";
    private static final String ADMIN_PANEL_SETTINGS_PATH =
            "M722.5-297.5Q740-315 740-340t-17.5-42.5Q705-400 680-400t-42.5 "
                    + "17.5Q620-365 620-340t17.5 42.5Q655-280 680-280t42.5-17.5ZM680-160q31 "
                    + "0 57-14.5t42-38.5q-22-13-47-20t-52-7q-27 0-52 7t-47 20q16 24 "
                    + "42 38.5t57 14.5ZM480-80q-139-35-229.5-159.5T160-516v-244l320-120 "
                    + "320 120v227q-19-8-39-14.5t-41-9.5v-147l-240-90-240 90v188q0 "
                    + "47 12.5 94t35 89.5Q310-290 342-254t71 60q11 32 29 61t41 "
                    + "52q-1 0-1.5.5t-1.5.5Zm200 0q-83 0-141.5-58.5T480-280q0-83 "
                    + "58.5-141.5T680-480q83 0 141.5 58.5T880-280q0 83-58.5 141.5T680-80ZM480-494Z";
    private static final String CHECK_BOX_CHECKED_PATH =
            "M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840h560"
                    + "q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120H200Z"
                    + "M424-312 706-594 650-650 424-424 310-538 254-482Z";
    private static final String CHECK_BOX_UNCHECKED_PATH =
            "M200-120q-33 0-56.5-23.5T120-200v-560q0-33 23.5-56.5T200-840"
                    + "h560q33 0 56.5 23.5T840-760v560q0 33-23.5 56.5T760-120"
                    + "H200Zm0-80h560v-560H200v560Z";

    private final SVGPath path = new SVGPath();
    private final StringProperty icon = new SimpleStringProperty(this, "icon");

    /**
     * Creates an empty icon. FXML sets the icon name after construction.
     */
    public SvgIcon() {
        path.getStyleClass().add("svg-icon-shape");
        path.setManaged(false);
        getChildren().add(path);
        icon.addListener((observable, oldValue, newValue) -> {
            path.setContent(resolvePath(newValue));
            path.setFillRule(resolveFillRule(newValue));
            requestLayout();
        });
    }

    public String getIcon() {
        return icon.get();
    }

    public void setIcon(final String value) {
        icon.set(value);
    }

    public StringProperty iconProperty() {
        return icon;
    }

    @Override
    protected void layoutChildren() {
        final Bounds bounds = path.getLayoutBounds();
        if (bounds.getWidth() <= 0 || bounds.getHeight() <= 0) {
            return;
        }

        final double scale = Math.min(getWidth() / bounds.getWidth(),
                getHeight() / bounds.getHeight());
        path.setScaleX(scale);
        path.setScaleY(scale);
        path.setTranslateX(0);
        path.setTranslateY(0);

        final Bounds scaledBounds = path.getBoundsInParent();
        path.setTranslateX((getWidth() - scaledBounds.getWidth()) / 2
                - scaledBounds.getMinX());
        path.setTranslateY((getHeight() - scaledBounds.getHeight()) / 2
                - scaledBounds.getMinY());
    }

    private static String resolvePath(final String iconName) {
        return switch (iconName) {
            case "category" -> CATEGORY_PATH;
            case "art" -> ART_PATH;
            case "electronic" -> ELECTRONIC_PATH;
            case "vehicle" -> VEHICLE_PATH;
            case "add" -> ADD_PATH;
            case "format-list-bulleted" -> FORMAT_LIST_BULLETED_PATH;
            case "admin-panel-settings" -> ADMIN_PANEL_SETTINGS_PATH;
            case "check-box-checked" -> CHECK_BOX_CHECKED_PATH;
            case "check-box-unchecked" -> CHECK_BOX_UNCHECKED_PATH;
            case null, default -> throw new IllegalArgumentException(
                    "Unsupported SVG icon: " + iconName);
        };
    }

    private static FillRule resolveFillRule(final String iconName) {
        return "check-box-checked".equals(iconName)
                ? FillRule.EVEN_ODD
                : FillRule.NON_ZERO;
    }
}
