package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
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

    // =========================================================================
    // PENDING CALLBACKS
    // =========================================================================

    // Mỗi request async hiện chỉ giữ một callback đang chờ response tương ứng.
    private FetchAuctionsCallback currentListCallback;
    private FetchAuctionDetailCallback currentDetailCallback;
    private PlaceBidCallback currentBidCallback;
    private AutoBidCallback currentAutoBidCallback;
    private AutoBidStatusCallback currentAutoBidStatusCallback;
    private FetchBidHistoryCallback currentBidHistoryCallback;

    // =========================================================================
    // SINGLETON LIFECYCLE
    // =========================================================================

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
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.AUTO_BID_OK.name(), this::handleAutoBidSuccess
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.AUTO_BID_FAIL.name(), this::handleAutoBidFailure
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.AUTO_BID_STATUS.name(), this::handleAutoBidStatus
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.BID_HISTORY.name(), this::handleBidHistoryResponse
        );
        NetworkClient.getInstance().registerHandler(
            Protocol.Response.BALANCE_UPDATED.name(), this::handleBalanceUpdated
        );
    }

    public static AuctionService getInstance() {
        return INSTANCE;
    }

    // =========================================================================
    // CALLBACK CONTRACTS
    // =========================================================================

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
     * Callback trả kết quả bật đấu giá tự động.
     */
    @FunctionalInterface
    public interface AutoBidCallback {
        void onResult(boolean isSuccess, String message);
    }

    /**
     * Callback nhận trạng thái auto-bid hiện tại của user trong một phiên.
     */
    @FunctionalInterface
    public interface AutoBidStatusCallback {
        void onResult(boolean enabled, long maxAmount, long stepAmount);
    }

    /**
     * Callback nhận lịch sử đặt giá đã được parse từ phản hồi BID_HISTORY.
     *
     * <p>Mỗi phần tử trong danh sách là một mảng theo thứ tự:
     * {@code time}, {@code bidder}, {@code amount}.
     */
    @FunctionalInterface
    public interface FetchBidHistoryCallback {
        void onResult(List<String[]> bidHistoryRows);
    }

    // =========================================================================
    // AUCTION LIST
    // =========================================================================

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
        currentListCallback.onResult(AuctionResponseParser.parseAuctionList(response));
        currentListCallback = null; // Giải phóng bộ nhớ
    }

    // =========================================================================
    // AUCTION DETAIL
    // =========================================================================

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
        currentDetailCallback.onResult(AuctionResponseParser.parseAuctionDetail(response));
        currentDetailCallback = null;
    }

    // =========================================================================
    // BID SUBMISSION
    // =========================================================================

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
     * Gửi yêu cầu bật auto-bid cho một phiên đấu giá.
     *
     * <p>Format request: {@code ENABLE_AUTO_BID|auctionId|maxAmount|stepAmount}.
     *
     * @param auctionId mã phiên đấu giá
     * @param maxAmount giá tối đa người dùng cho phép auto-bid
     * @param stepAmount bước tăng mỗi lần bị vượt giá
     * @param callback callback nhận kết quả từ server
     */
    public void enableAutoBid(
            final String auctionId,
            final long maxAmount,
            final long stepAmount,
            final AutoBidCallback callback) {

        this.currentAutoBidCallback = callback;

        final String request = Protocol.Command.ENABLE_AUTO_BID.name()
                + Protocol.SEPARATOR + auctionId
                + Protocol.SEPARATOR + maxAmount
                + Protocol.SEPARATOR + stepAmount;

        NetworkClient.getInstance().sendCommand(request);
    }

    /**
     * Gửi yêu cầu tắt auto-bid cho một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá
     * @param callback callback nhận kết quả từ server
     */
    public void disableAutoBid(
            final String auctionId,
            final AutoBidCallback callback) {

        this.currentAutoBidCallback = callback;

        final String request = Protocol.Command.DISABLE_AUTO_BID.name()
                + Protocol.SEPARATOR + auctionId;

        NetworkClient.getInstance().sendCommand(request);
    }

    /**
     * Gửi yêu cầu lấy trạng thái auto-bid hiện tại của user trong phiên.
     *
     * @param auctionId mã phiên đấu giá
     * @param callback callback nhận enabled/maxAmount/stepAmount
     */
    public void fetchAutoBidStatus(
            final String auctionId,
            final AutoBidStatusCallback callback) {

        this.currentAutoBidStatusCallback = callback;

        final String request = Protocol.Command.GET_AUTO_BID.name()
                + Protocol.SEPARATOR + auctionId;

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
        OptionalDouble parsedBalance = AuctionResponseParser.parseBidOkBalance(response);
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
        String message = AuctionResponseParser.parseBidFailureMessage(response);
        
        currentBidCallback.onResult(false, message, 0);
        currentBidCallback = null;
    }

    /**
     * Xử lý phản hồi bật auto-bid thành công.
     *
     * @param response phản hồi AUTO_BID_OK từ server
     */
    private void handleAutoBidSuccess(final String response) {
        if (currentAutoBidCallback == null) {
            return;
        }

        LOGGER.info("AuctionService bật auto-bid thành công: " + response);
        currentAutoBidCallback.onResult(true, parseSimpleSuccessMessage(response));
        currentAutoBidCallback = null;
    }

    /**
     * Xử lý phản hồi bật auto-bid thất bại.
     *
     * @param response phản hồi AUTO_BID_FAIL từ server
     */
    private void handleAutoBidFailure(final String response) {
        if (currentAutoBidCallback == null) {
            return;
        }

        LOGGER.warn("AuctionService bật auto-bid thất bại: " + response);
        currentAutoBidCallback.onResult(false, parseSimpleFailureMessage(response));
        currentAutoBidCallback = null;
    }

    /**
     * Xử lý phản hồi trạng thái auto-bid hiện tại.
     *
     * @param response phản hồi AUTO_BID_STATUS từ server
     */
    private void handleAutoBidStatus(final String response) {
        if (currentAutoBidStatusCallback == null) {
            return;
        }

        final String[] parts = response.split(Protocol.SEPARATOR_REGEX);
        if (parts.length >= 4 && "ENABLED".equals(parts[1])) {
            currentAutoBidStatusCallback.onResult(
                    true,
                    parseLongOrZero(parts[2]),
                    parseLongOrZero(parts[3]));
        } else {
            currentAutoBidStatusCallback.onResult(false, 0L, 0L);
        }

        currentAutoBidStatusCallback = null;
    }

    /**
     * Lấy message từ response thành công dạng {@code RESPONSE|message}.
     *
     * @param response phản hồi từ server
     * @return message thành công nếu có, hoặc thông báo mặc định
     */
    private String parseSimpleSuccessMessage(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, 2);
        return parts.length >= 2 ? parts[1] : "Yêu cầu thành công.";
    }

    /**
     * Lấy message từ response dạng {@code RESPONSE|message}.
     *
     * @param response phản hồi từ server
     * @return message lỗi nếu có, hoặc thông báo mặc định
     */
    private String parseSimpleFailureMessage(final String response) {
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, 2);
        return parts.length >= 2 ? parts[1] : "Yêu cầu không thành công.";
    }

    private long parseLongOrZero(final String rawValue) {
        try {
            return Long.parseLong(rawValue);
        } catch (NumberFormatException exception) {
            return 0L;
        }
    }

    // =========================================================================
    // BID HISTORY
    // =========================================================================

    /**
     * Gửi yêu cầu lấy lịch sử đặt giá của một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá cần lấy lịch sử
     * @param callback callback nhận danh sách dòng lịch sử đặt giá
     */
    public void fetchBidHistory(
            final String auctionId,
            final FetchBidHistoryCallback callback) {
        this.currentBidHistoryCallback = callback;

        NetworkClient.getInstance().sendCommand(
            Protocol.Command.GET_BID_HISTORY.name()
                    + Protocol.SEPARATOR
                    + auctionId
        );
    }

    /**
     * Bóc tách phản hồi lịch sử bid từ server.
     *
     * <p>Record đầu tiên là header {@code BID_HISTORY|auctionId|count}; các
     * record còn lại mới là từng dòng dữ liệu để bảng/biểu đồ sử dụng.
     *
     * @param response phản hồi BID_HISTORY dạng chuỗi theo protocol socket
     */
    private void handleBidHistoryResponse(final String response) {
        if (currentBidHistoryCallback == null) {
            return;
        }

        LOGGER.info("AuctionService nhận lịch sử bid: " + response);
        currentBidHistoryCallback.onResult(AuctionResponseParser.parseBidHistory(response));
        currentBidHistoryCallback = null;
    }

    /**
     * Xử lý realtime cập nhật ví khi số dư user thay đổi do bid.
     *
     * <p>Trường hợp chính là user đang dẫn đầu bị người khác vượt giá, server hoàn
     * tiền và gửi số dư mới xuống đúng socket của user đó.
     *
     * @param response response theo format BALANCE_UPDATED|userId|newBalance
     */
    private void handleBalanceUpdated(final String response) {
        OptionalDouble parsedBalance = AuctionResponseParser.parseBalanceUpdate(response);
        if (parsedBalance.isPresent()) {
            // Cập nhật nguồn dữ liệu chung; ProfileController đang listen property này.
            UserSessionService.getInstance().updateCurrentUserBalance(parsedBalance.getAsDouble());
        }
    }

}
