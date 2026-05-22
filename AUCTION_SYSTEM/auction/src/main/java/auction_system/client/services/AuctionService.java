package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service xử lý các tác vụ liên quan đến quản lý phiên đấu giá ở phía Client.
 * Áp dụng mẫu thiết kế Singleton để duy trì một điểm truy cập duy nhất.
 */
public class AuctionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionService.class);
    private static final AuctionService INSTANCE = new AuctionService();
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
        void onResult(boolean isSuccess, String message);
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
        String[] lines = response.split(Protocol.RECORD_SEPARATOR);

        for (int i = 1; i < lines.length; ++i) {
            String[] parts = lines[i].split(Protocol.SEPARATOR_REGEX);
            if (parts.length >= 5) {
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
        // BID_OK|auctionId|amount
        currentBidCallback.onResult(true, "Đặt giá thành công!");
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
        String message = (parts.length > 1) ? parts[1] : "Lỗi đặt giá không xác định.";
        
        currentBidCallback.onResult(false, message);
        currentBidCallback = null;
    }

}
