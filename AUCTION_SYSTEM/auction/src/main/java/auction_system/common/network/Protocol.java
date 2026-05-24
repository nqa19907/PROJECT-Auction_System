package auction_system.common.network;

/**
 * Lớp chứa tất cả các lệnh và phản hồi giao tiếp qua Socket.
 */
public final class Protocol {

    private Protocol() {
        // Ngăn chặn khởi tạo lớp tiện ích từ bên ngoài
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    // --- KÝ TỰ NGĂN CÁCH ---
    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEX = "\\|";
    public static final String RECORD_SEPARATOR = "~";

    /**
     * Định nghĩa các mã lệnh gửi từ Client lên Server.
     */
    public enum Command {
        // Auth
        LOGIN,
        REGISTER,
        LOGOUT,
        DEPOSIT,
        
        // Auction
        LIST_AUCTIONS,
        GET_AUCTION,
        GET_BID_HISTORY,
        WATCH_AUCTION,
        UNWATCH_AUCTION,
        PLACE_BID,

        // Admin
        ADMIN_LIST_USERS,
        ADMIN_LIST_AUCTIONS,
        ADMIN_CANCEL_AUCTION,
        ADMIN_DELETE_AUCTION,
        ADMIN_DELETE_USER,
        PUBLISH_ITEM
    }

    /**
     * Định nghĩa các mã phản hồi gửi từ Server về Client.
     */
    public enum Response {
        ERROR,
        
        // Auth Responses
        LOGIN_OK, LOGIN_FAIL,
        REGISTER_OK, REGISTER_FAIL,
        LOGOUT_OK,
        DEPOSIT_OK, DEPOSIT_FAIL,
        
        // Auction Responses
        AUCTION_LIST,
        AUCTION_DETAIL,
        WATCH_OK, WATCH_FAIL,
        BID_HISTORY,
        UNWATCH_OK,
        BID_OK, BID_FAIL,
        PUBLISH_ITEM_OK, PUBLISH_ITEM_FAIL,

        // Broadcast Updates
        UPDATE_PRICE,
        BALANCE_UPDATED,
        AUCTION_STARTED,
        AUCTION_ENDED,

        // Admin responses
        ADMIN_USER_LIST,
        ADMIN_USER_LIST_FAIL,
        ADMIN_AUCTION_LIST,
        ADMIN_AUCTION_LIST_FAIL,
        ADMIN_CANCEL_AUCTION_OK,
        ADMIN_CANCEL_AUCTION_FAIL,
        ADMIN_DELETE_AUCTION_OK,
        ADMIN_DELETE_AUCTION_FAIL,
        ADMIN_DELETE_USER_OK,
        ADMIN_DELETE_USER_FAIL

    }
}
