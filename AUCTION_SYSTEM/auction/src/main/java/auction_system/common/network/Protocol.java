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
        
        // Auction
        PUBLISH_ITEM,
        LIST_AUCTIONS,
        GET_AUCTION,
        JOIN_AUCTION,
        LEAVE_AUCTION,
        PLACE_BID
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
        
        // Auction Responses
        AUCTION_LIST,
        AUCTION_DETAIL,
        JOIN_OK, JOIN_FAIL,
        LEAVE_OK,
        BID_OK, BID_FAIL,
        PUBLISH_ITEM_OK,
        PUBLISH_ITEM_FAIL,

        // Broadcast Updates
        UPDATE_PRICE,
        AUCTION_STARTED,
        AUCTION_ENDED
    }
}