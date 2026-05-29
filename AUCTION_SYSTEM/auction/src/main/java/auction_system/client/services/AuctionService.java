package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import com.fasterxml.jackson.core.JsonProcessingException;
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

    private final AuctionResponseParser responseParser = new AuctionResponseParser();
    private final AuctionNotificationHandler notificationHandler =
            new AuctionNotificationHandler();

    // Mỗi request async hiện chỉ giữ một callback đang chờ response tương ứng.
    private FetchAuctionsCallback currentListCallback;
    private FetchAuctionDetailCallback currentDetailCallback;
    private PlaceBidCallback currentBidCallback;
    private AutoBidCallback currentAutoBidCallback;
    private AutoBidStatusCallback currentAutoBidStatusCallback;
    private FetchBidHistoryCallback currentBidHistoryCallback;

    /**
     * Khởi tạo AuctionService ẩn (private) và tự động đăng ký bộ lắng nghe lệnh phản hồi.
     */
    private AuctionService() {
        registerAuctionQueryHandlers();
        registerBidHandlers();
        registerRealtimeNotificationHandlers();
    }

    public static AuctionService getInstance() {
        return INSTANCE;
    }

    /**
     * Đăng ký client hiện tại theo dõi realtime một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá cần theo dõi
     */
    public void joinAuction(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        NetworkClient.getInstance().sendCommand(buildJoinAuctionRequest(auctionId));
    }

    /**
     * Hủy đăng ký theo dõi realtime một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá cần rời khỏi
     */
    public void leaveAuction(final String auctionId) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        NetworkClient.getInstance().sendCommand(buildLeaveAuctionRequest(auctionId));
    }

    /**
     * Gửi yêu cầu bật/tắt tự động gia hạn khi có bid phút chót cho một phiên đấu giá.
     *
     * @param auctionId mã phiên đấu giá cần cập nhật
     * @param enabled true nếu bật tự động gia hạn phút chót
     */
    public void setAntiSniping(final String auctionId, final boolean enabled) {
        if (auctionId == null || auctionId.isBlank()) {
            return;
        }

        NetworkClient.getInstance().sendCommand(buildSetAntiSnipingRequest(auctionId, enabled));
    }

    private String buildJoinAuctionRequest(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.JOIN_AUCTION.name(),
                            null,
                            JsonProtocol.payloadOf(auctionId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request tham gia phiên: {}",
                    exception.getMessage());
            return Protocol.Command.JOIN_AUCTION.name()
                    + Protocol.SEPARATOR
                    + auctionId;
        }
    }

    private String buildLeaveAuctionRequest(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.LEAVE_AUCTION.name(),
                            null,
                            JsonProtocol.payloadOf(auctionId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request rời phiên: {}",
                    exception.getMessage());
            return Protocol.Command.LEAVE_AUCTION.name()
                    + Protocol.SEPARATOR
                    + auctionId;
        }
    }

    private String buildSetAntiSnipingRequest(final String auctionId, final boolean enabled) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.SET_ANTI_SNIPING.name(),
                            null,
                            JsonProtocol.payloadOf(List.of(auctionId, enabled)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request chống đặt giá phút chót: {}",
                    exception.getMessage());
            return Protocol.Command.SET_ANTI_SNIPING.name()
                    + Protocol.SEPARATOR
                    + auctionId
                    + Protocol.SEPARATOR
                    + enabled;
        }
    }

    private String buildPlaceBidRequest(final String auctionId, final double amount) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.PLACE_BID.name(),
                            null,
                            JsonProtocol.payloadOf(List.of(auctionId, amount)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request đặt giá: {}", exception.getMessage());
            return Protocol.Command.PLACE_BID.name()
                    + Protocol.SEPARATOR + auctionId
                    + Protocol.SEPARATOR + amount;
        }
    }

    private String buildEnableAutoBidRequest(
            final String auctionId,
            final long maxAmount,
            final long stepAmount) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.ENABLE_AUTO_BID.name(),
                            null,
                            JsonProtocol.payloadOf(List.of(auctionId, maxAmount, stepAmount)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request bật auto-bid: {}",
                    exception.getMessage());
            return Protocol.Command.ENABLE_AUTO_BID.name()
                    + Protocol.SEPARATOR + auctionId
                    + Protocol.SEPARATOR + maxAmount
                    + Protocol.SEPARATOR + stepAmount;
        }
    }

    private String buildDisableAutoBidRequest(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.DISABLE_AUTO_BID.name(),
                            null,
                            JsonProtocol.payloadOf(auctionId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request tắt auto-bid: {}",
                    exception.getMessage());
            return Protocol.Command.DISABLE_AUTO_BID.name()
                    + Protocol.SEPARATOR + auctionId;
        }
    }

    private String buildGetAutoBidRequest(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.GET_AUTO_BID.name(),
                            null,
                            JsonProtocol.payloadOf(auctionId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request lấy trạng thái auto-bid: {}",
                    exception.getMessage());
            return Protocol.Command.GET_AUTO_BID.name()
                    + Protocol.SEPARATOR + auctionId;
        }
    }

    private String buildListAuctionsRequest() {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.LIST_AUCTIONS.name(),
                            null,
                            null,
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request lấy danh sách phiên: {}",
                    exception.getMessage());
            return Protocol.Command.LIST_AUCTIONS.name();
        }
    }

    private String buildGetAuctionRequest(final String auctionId) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            null,
                            Protocol.Command.GET_AUCTION.name(),
                            null,
                            JsonProtocol.payloadOf(auctionId),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON request lấy chi tiết phiên: {}",
                    exception.getMessage());
            return Protocol.Command.GET_AUCTION.name()
                    + Protocol.SEPARATOR
                    + auctionId;
        }
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

    /**
     * Gửi yêu cầu lấy danh sách phiên đấu giá hiện có lên Server.
     *
     * @param callback Hàm callback để xử lý danh sách nhận được trả về từ Server.
     */
    public void fetchAuctionList(final FetchAuctionsCallback callback) {
        this.currentListCallback = callback;
        NetworkClient.getInstance().sendCommand(buildListAuctionsRequest());
    }

    /**
     * Gửi yêu cầu lấy thông tin chi tiết một phiên đấu giá.
     *
     * @param auctionId Mã phiên đấu giá cần lấy chi tiết.
     * @param callback Hàm xử lý kết quả trả về.
     */
    public void fetchAuctionDetail(
            final String auctionId,
            final FetchAuctionDetailCallback callback) {

        this.currentDetailCallback = callback;
        NetworkClient.getInstance().sendCommand(buildGetAuctionRequest(auctionId));
    }

    /**
     * Gửi yêu cầu đặt giá cho một phiên đấu giá.
     *
     * @param auctionId Mã phiên đấu giá.
     * @param amount Số tiền đặt giá.
     * @param callback Hàm xử lý kết quả đặt giá trả về.
     */
    public void placeBid(
            final String auctionId,
            final double amount,
            final PlaceBidCallback callback) {

        this.currentBidCallback = callback;

        NetworkClient.getInstance().sendCommand(buildPlaceBidRequest(auctionId, amount));
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

        NetworkClient.getInstance().sendCommand(
                buildEnableAutoBidRequest(auctionId, maxAmount, stepAmount));
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

        NetworkClient.getInstance().sendCommand(buildDisableAutoBidRequest(auctionId));
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

        NetworkClient.getInstance().sendCommand(buildGetAutoBidRequest(auctionId));
    }

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
                        + auctionId);
    }

    /**
     * Đăng ký handler cho các response truy vấn dữ liệu đấu giá.
     */
    private void registerAuctionQueryHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_LIST.name(), this::handleAuctionListResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_DETAIL.name(), this::handleAuctionDetailResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.BID_HISTORY.name(), this::handleBidHistoryResponse);
    }

    /**
     * Đăng ký handler cho các response liên quan đến đặt giá và auto-bid.
     */
    private void registerBidHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.BID_OK.name(), this::handlePlaceBidSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.BID_FAIL.name(), this::handlePlaceBidFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUTO_BID_OK.name(), this::handleAutoBidSuccess);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUTO_BID_FAIL.name(), this::handleAutoBidFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUTO_BID_STATUS.name(), this::handleAutoBidStatus);
    }

    /**
     * Đăng ký handler cho các thông báo realtime không gắn với callback request.
     */
    private void registerRealtimeNotificationHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_ENDED.name(),
                notificationHandler::handleAuctionEndedResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_WINNER.name(),
                notificationHandler::handleAuctionWinnerResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.AUCTION_LOST.name(),
                notificationHandler::handleAuctionLostResponse);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.BALANCE_UPDATED.name(),
                notificationHandler::handleBalanceUpdatedResponse);
    }

    /**
     * Bóc tách và xử lý dữ liệu thô nhận được từ Server sau khi gửi lệnh xin danh sách đấu giá.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handleAuctionListResponse(final String response) {
        if (currentListCallback == null) {
            return;
        }

        LOGGER.info("AuctionService xử lý phản hồi: " + response);

        // Parser lọc bỏ header và chỉ trả về các record đủ field cho bảng đấu giá.
        List<String[]> auctionList = responseParser.parseAuctionList(response);
        currentListCallback.onResult(auctionList);
        currentListCallback = null;
    }

    /**
     * Bóc tách phản hồi chi tiết phiên đấu giá từ Server.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handleAuctionDetailResponse(final String response) {
        if (currentDetailCallback == null) {
            return;
        }

        LOGGER.info("AuctionService nhận chi tiết: " + response);

        // Controller/ViewModel phía trên đang chịu trách nhiệm hiểu thứ tự các field chi tiết.
        currentDetailCallback.onResult(responseParser.parseAuctionDetail(response));
        currentDetailCallback = null;
    }

    /**
     * Xử lý khi đặt giá thành công.
     *
     * @param response Chuỗi phản hồi từ mạng do Server trả về.
     */
    private void handlePlaceBidSuccess(final String response) {
        if (currentBidCallback == null) {
            return;
        }

        LOGGER.info("AuctionService đặt giá thành công: " + response);

        // Số dư phải lấy từ server vì server xử lý hoàn/trừ tiền khi bid thay đổi.
        OptionalDouble parsedBalance = responseParser.parseBidBalance(response);
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
    private void handlePlaceBidFailure(final String response) {
        if (currentBidCallback == null) {
            return;
        }

        LOGGER.warn("AuctionService đặt giá thất bại: " + response);

        // Server cũ hoặc lỗi mạng có thể trả thiếu message, nên fallback message vẫn cần có.
        String message = responseParser.parseBidFailureMessage(response);
        currentBidCallback.onResult(false, message, 0);
        currentBidCallback = null;
    }

    /**
     * Xử lý phản hồi bật/tắt auto-bid thành công.
     *
     * @param response phản hồi AUTO_BID_OK từ server
     */
    private void handleAutoBidSuccess(final String response) {
        if (currentAutoBidCallback == null) {
            return;
        }

        LOGGER.info("AuctionService bật auto-bid thành công: " + response);
        currentAutoBidCallback.onResult(true, responseParser.parseSimpleSuccessMessage(response));
        currentAutoBidCallback = null;
    }

    /**
     * Xử lý phản hồi bật/tắt auto-bid thất bại.
     *
     * @param response phản hồi AUTO_BID_FAIL từ server
     */
    private void handleAutoBidFailure(final String response) {
        if (currentAutoBidCallback == null) {
            return;
        }

        LOGGER.warn("AuctionService bật auto-bid thất bại: " + response);
        currentAutoBidCallback.onResult(false, responseParser.parseSimpleFailureMessage(response));
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

        // Response ENABLED có thêm maxAmount/stepAmount; các trạng thái khác xem như tắt.
        AuctionResponseParser.AutoBidStatus status = responseParser.parseAutoBidStatus(response);
        currentAutoBidStatusCallback.onResult(
                status.enabled(),
                status.maxAmount(),
                status.stepAmount());
        currentAutoBidStatusCallback = null;
    }

    /**
     * Bóc tách phản hồi lịch sử bid từ server.
     *
     * @param response phản hồi BID_HISTORY dạng chuỗi theo protocol socket
     */
    private void handleBidHistoryResponse(final String response) {
        if (currentBidHistoryCallback == null) {
            return;
        }

        LOGGER.info("AuctionService nhận lịch sử bid: " + response);

        // Parser bỏ header BID_HISTORY|auctionId|count và giữ lại từng dòng bid hợp lệ.
        List<String[]> bidHistoryRows = responseParser.parseBidHistory(response);
        currentBidHistoryCallback.onResult(bidHistoryRows);
        currentBidHistoryCallback = null;
    }
}
