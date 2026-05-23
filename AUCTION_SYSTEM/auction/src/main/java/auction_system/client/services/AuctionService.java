package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý các tác vụ liên quan đến quản lý phiên đấu giá ở phía Client.
 * Áp dụng mẫu thiết kế Singleton để duy trì một điểm truy cập duy nhất.
 */
public class AuctionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionService.class);
    private static final AuctionService INSTANCE = new AuctionService();

    // AUCTION_LIST có dòng đầu là header, dữ liệu phiên bắt đầu từ dòng tiếp theo.
    private static final int FIRST_AUCTION_RECORD_INDEX = 1;

    // Format tối thiểu: auctionId|itemName|currentPrice|status|endTime.
    private static final int MIN_AUCTION_LIST_PARTS = 5;

    // BID_FAIL|message.
    private static final int MIN_BID_FAIL_PARTS = 2;
    private static final int IDX_BID_FAIL_MESSAGE = 1;

    // BID_OK|auctionId|amount|newBalance.
    private static final int MIN_BID_OK_PARTS = 4;
    private static final int IDX_BID_NEW_BALANCE = 3;

    // Mỗi request async hiện chỉ giữ một callback đang chờ response tương ứng.
    private FetchAuctionsCallback currentListCallback;
    private FetchAuctionDetailCallback currentDetailCallback;
    private PlaceBidCallback currentBidCallback;

    /**
     * Khởi tạo AuctionService ẩn (private) và tự động đăng ký bộ lắng nghe lệnh phản hồi.
     */
    private AuctionService() {
        // Đăng ký "lắng nghe" Server ngay khi Service được tạo ra
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.AUCTION_LIST.name(), this::handleAuctionListResponse
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.AUCTION_DETAIL.name(), this::handleAuctionDetailResponse
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.BID_OK.name(), this::handlePlaceBidSuccess
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.BID_FAIL.name(), this::handlePlaceBidFailure
        );
    }

    public static AuctionService getInstance() {
        return INSTANCE;
    }


    /**
     * Định nghĩa giao diện Callback trả dữ liệu về Controller.
     */
    @FunctionalInterface
    public interface FetchAuctionsCallback {
        void onResult(List<String[]> auctionDataList);
    }

    /**
     * Định nghĩa giao diện Callback trả dữ liệu chi tiết về Controller.
     */
    @FunctionalInterface
    public interface FetchAuctionDetailCallback {
        void onResult(String[] auctionDetailData);
    }

    /**
     * Định nghĩa giao diện Callback trả kết quả đặt giá về Controller.
     */
    @FunctionalInterface
    public interface PlaceBidCallback {
        void onResult(boolean isSuccess, String message, double newBalance);
    }

    /**
     * Gửi yêu cầu lấy danh sách phiên đấu giá hiện có lên Server.
     *
     * @param callback Hàm callback để xử lý danh sách nhận được trả về từ Server.
     */
    public void fetchAuctionList(FetchAuctionsCallback callback) {
        this.currentListCallback = callback;
        NetworkClient.getInstance().sendCommand(
            Protocol.Command.LIST_AUCTIONS.name()
        );
    }

    /**
     * Bóc tách và xử lý dữ liệu thô nhận được từ Server sau khi gửi lệnh xin danh sách đấu giá.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handleAuctionListResponse(String response) {
        if (currentListCallback == null) {
            return;
        }

        LOGGER.info("AuctionService xử lý phản hồi: " + response);
        List<String[]> auctionList = new ArrayList<>();

        // AUCTION_LIST là response nhiều dòng, khác với phần lớn response một dòng.
        String[] lines = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = FIRST_AUCTION_RECORD_INDEX; i < lines.length; ++i) {
            String[] parts = lines[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= MIN_AUCTION_LIST_PARTS) {
                auctionList.add(parts);
            }
        }

        currentListCallback.onResult(auctionList);
        currentListCallback = null; // Giải phóng bộ nhớ
    }

    /**
     * Gửi yêu cầu lấy thông tin chi tiết một phiên đấu giá.
     *
     * @param auctionId Mã phiên đấu giá cần lấy chi tiết.
     * @param callback Hàm xử lý kết quả trả về.
     */
    public void fetchAuctionDetail(String auctionId, FetchAuctionDetailCallback callback) {
        this.currentDetailCallback = callback;
        NetworkClient.getInstance().sendCommand(
            Protocol.Command.GET_AUCTION.name() + Protocol.SEPARATOR + auctionId
        );
    }

    /**
     * Bóc tách phản hồi chi tiết phiên đấu giá từ Server.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handleAuctionDetailResponse(String response) {
        if (currentDetailCallback == null) {
            return;
        }
        LOGGER.info("AuctionService nhận chi tiết: " + response);

        // Controller/ViewModel phía trên đang chịu trách nhiệm hiểu thứ tự các field chi tiết.
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        currentDetailCallback.onResult(parts);
        currentDetailCallback = null;
    }

    /**
     * Gửi yêu cầu đặt giá cho một phiên đấu giá.
     *
     * @param auctionId Mã phiên đấu giá.
     * @param amount Số tiền đặt giá.
     * @param callback Hàm xử lý kết quả đặt giá trả về.
     */
    public void placeBid(String auctionId, double amount, PlaceBidCallback callback) {
        this.currentBidCallback = callback;

        // Giữ format request đúng protocol server: PLACE_BID|auctionId|amount.
        String request = Protocol.Command.PLACE_BID.name()
                + Protocol.SEPARATOR + auctionId
                + Protocol.SEPARATOR + amount;
        NetworkClient.getInstance().sendCommand(request);
    }

    /**
     * Xử lý khi đặt giá thành công.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handlePlaceBidSuccess(String response) {
        if (currentBidCallback == null) {
            return;
        }
        LOGGER.info("AuctionService đặt giá thành công: " + response);
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        // BID_OK|auctionId|amount|newBalance.
        // Số dư phải lấy từ server vì server xử lý hoàn/trừ tiền.
        OptionalDouble parsedBalance = parseBidBalance(parts);
        if (parsedBalance.isEmpty()) {
            currentBidCallback.onResult(false, "Phản hồi đặt giá không hợp lệ.", 0);
            currentBidCallback = null;
            return;
        }

        double newBalance = parsedBalance.getAsDouble();
        UserSessionService.getInstance().updateCurrentUserBalance(newBalance);

        currentBidCallback.onResult(true, "Đặt giá thành công!", newBalance);
        currentBidCallback = null;
    }

    /**
     * Xử lý khi đặt giá thất bại.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handlePlaceBidFailure(String response) {
        if (currentBidCallback == null) {
            return;
        }
        LOGGER.warn("AuctionService đặt giá thất bại: " + response);
        // BID_FAIL|message
        String[] parts = response.split(Protocol.SEPARATOR_REGEX);

        // Server cũ hoặc lỗi mạng có thể trả thiếu message, nên fallback message vẫn cần có.
        String message = (parts.length >= MIN_BID_FAIL_PARTS)
                ? parts[IDX_BID_FAIL_MESSAGE] : "Lỗi đặt giá không xác định.";
        
        currentBidCallback.onResult(false, message, 0);
        currentBidCallback = null;
    }

    /**
     * Đọc số dư mới từ response BID_OK.
     *
     * <p>Trả về rỗng khi response không đúng format để tránh cập nhật sai
     * số dư hiện tại của user.
     *
     * @param parts response đã tách theo ký tự phân cách protocol
     * @return số dư mới nếu parse được
     */
    private OptionalDouble parseBidBalance(String[] parts) {
        if (parts.length < MIN_BID_OK_PARTS) {
            LOGGER.warn("Phản hồi BID_OK thiếu số dư mới.");
            return OptionalDouble.empty();
        }

        try {
            return OptionalDouble.of(Double.parseDouble(parts[IDX_BID_NEW_BALANCE]));
        } catch (NumberFormatException e) {
            LOGGER.warn(
                    "Không thể đọc số dư mới từ phản hồi BID_OK: {}",
                    parts[IDX_BID_NEW_BALANCE]);
            return OptionalDouble.empty();
        }
    }

}
