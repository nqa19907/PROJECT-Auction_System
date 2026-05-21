package auction_system.client.utils;

/**
 * Các đường dẫn đến các file View (FXML).
 */
public final class ViewConstants {

    private ViewConstants() {
        // Private constructor to prevent instantiation
    }

    // Thư mục gốc chứa các file giao diện
    public static final String BASE_PATH = "/client/fxml/";

    // Auth Views
    public static final String LOGIN_VIEW = BASE_PATH + "auth/Login.fxml";
    public static final String REGISTER_VIEW = BASE_PATH + "auth/Register.fxml";

    // Dashboard View
    public static final String DASHBOARD_VIEW = BASE_PATH + "dashboard/Dashboard.fxml";

    // Auction Views
    public static final String ITEM_LIST_VIEW = BASE_PATH + "auction/ItemList.fxml";
    public static final String PUBLISH_ITEM_VIEW = BASE_PATH + "auction/PublishItem.fxml";
    public static final String AUCTION_DETAIL_VIEW = BASE_PATH + "auction/AuctionDetail.fxml";

    // Profile Views
    public static final String HISTORY_VIEW = BASE_PATH + "profile/History.fxml";

    // Component Views (These might be loaded differently, but good to have)
    public static final String PROFILE_VIEW = BASE_PATH + "components/Profile.fxml";
    public static final String SIDEBAR_VIEW = BASE_PATH + "components/Sidebar.fxml";
    public static final String PRODUCT_CARD_VIEW = BASE_PATH + "components/ProductCard.fxml";
}