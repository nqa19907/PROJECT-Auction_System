package auction_system.common.network;

/**
 * Lớp chứa tất cả các lệnh và phản hồi giao tiếp qua Socket.
 */
public class Protocol {

    private Protocol() {
        // Ngăn chặn khởi tạo lớp tiện ích từ bên ngoài
    }

    // --- CÁC LỆNH TỪ CLIENT GỬI LÊN ---
    public static final String CMD_LOGIN = "LOGIN";
    public static final String CMD_REGISTER = "REGISTER";
    public static final String CMD_LIST_AUCTIONS = "LIST_AUCTIONS";
    public static final String CMD_GET_AUCTION = "GET_AUCTION";
    public static final String CMD_JOIN_AUCTION = "JOIN_AUCTION";
    public static final String CMD_LEAVE_AUCTION = "LEAVE_AUCTION";
    public static final String CMD_PLACE_BID = "PLACE_BID";
    public static final String CMD_LOGOUT = "LOGOUT";

    // --- CÁC PHẢN HỒI TỪ SERVER TRẢ VỀ ---
    public static final String RES_ERROR = "ERROR";
    public static final String RES_LOGIN_OK = "LOGIN_OK";
    public static final String RES_LOGIN_FAIL = "LOGIN_FAIL";
    public static final String RES_REGISTER_OK = "REGISTER_OK";
    public static final String RES_REGISTER_FAIL = "REGISTER_FAIL";
    public static final String RES_AUCTION_LIST = "AUCTION_LIST";
    public static final String RES_AUCTION_DETAIL = "AUCTION_DETAIL";
    public static final String RES_JOIN_OK = "JOIN_OK";
    public static final String RES_JOIN_FAIL = "JOIN_FAIL";
    public static final String RES_LEAVE_OK = "LEAVE_OK";
    public static final String RES_BID_OK = "BID_OK";
    public static final String RES_BID_FAIL = "BID_FAIL";
    public static final String RES_LOGOUT_OK = "LOGOUT_OK";
    
    // --- KÝ TỰ NGĂN CÁCH ---
    public static final String SEPARATOR = "|";
    public static final String SEPARATOR_REGEX = "\\|";
}