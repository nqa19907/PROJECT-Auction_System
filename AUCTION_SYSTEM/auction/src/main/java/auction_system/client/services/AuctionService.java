package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service xử lý các tác vụ liên quan đến quản lý phiên đấu giá ở phía Client.
 * Áp dụng mẫu thiết kế Singleton để duy trì một điểm truy cập duy nhất.
 */
public class AuctionService {
    private static final Logger LOGGER = Logger.getLogger(AuctionService.class.getName());
    private static final AuctionService INSTANCE = new AuctionService();
    private FetchAuctionsCallback currentListCallback;
    private FetchAuctionDetailCallback currentDetailCallback;

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
}
