package auction_system.client.services;

import auction_system.client.network.NetworkClient;
import auction_system.common.network.Protocol;
import java.util.ArrayList;
import java.util.List;

/**
 * Client service cho các thao tác của Admin Dashboard.
 *
 * <p>Service này giữ protocol parsing ở tầng service để controller không phụ
 * thuộc trực tiếp vào command/response string hoặc lớp server.
 */
public final class AdminDashboardService {

    private static final AdminDashboardService INSTANCE = new AdminDashboardService();

    private static final int FIRST_RECORD_INDEX = 1;
    private static final int MIN_USER_PARTS = 4;
    private static final int MIN_AUCTION_PARTS = 5;
    private static final int IDX_ID = 0;
    private static final int IDX_USER_USERNAME = 1;
    private static final int IDX_USER_EMAIL = 2;
    private static final int IDX_USER_STATUS = 3;
    private static final int IDX_AUCTION_PRODUCT = 1;
    private static final int IDX_AUCTION_SELLER = 2;
    private static final int IDX_AUCTION_PRICE = 3;
    private static final int IDX_AUCTION_STATUS = 4;
    private static final int IDX_RESPONSE_VALUE = 1;

    /*
     * NetworkClient dispatch response theo loại message, không theo request id.
     * Vì dashboard chỉ gửi từng thao tác quản trị một lần, service giữ callback
     * đang chờ để ghép response tiếp theo về đúng luồng UI đã gọi.
     */
    private AdminUsersCallback currentUsersCallback;
    private AdminAuctionsCallback currentAuctionsCallback;
    private AdminDeleteCallback currentDeleteUserCallback;
    private AdminDeleteCallback currentDeleteAuctionCallback;

    private AdminDashboardService() {
        /*
         * Service singleton đăng ký handler một lần cho toàn app. Controller chỉ
         * gọi fetch/delete và nhận callback, không tự thao tác với protocol socket.
         */
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_USER_LIST.name(),
                this::handleUserList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_USER_LIST_FAIL.name(),
                this::handleUserListFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_AUCTION_LIST.name(),
                this::handleAuctionList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name(),
                this::handleAuctionListFailure);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_OK.name(),
                this::handleDeleteUserOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_FAIL.name(),
                this::handleDeleteUserFail);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_OK.name(),
                this::handleDeleteAuctionOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name(),
                this::handleDeleteAuctionFail);
    }

    public static AdminDashboardService getInstance() {
        return INSTANCE;
    }

    /**
     * Callback nhận kết quả tải danh sách user.
     */
    @FunctionalInterface
    public interface AdminUsersCallback {
        void onResult(boolean success, String message, List<UserSnapshot> users);
    }

    /**
     * Callback nhận kết quả tải danh sách auction.
     */
    @FunctionalInterface
    public interface AdminAuctionsCallback {
        void onResult(boolean success, String message, List<AuctionSnapshot> auctions);
    }

    /**
     * Callback nhận kết quả thao tác xóa dữ liệu quản trị.
     */
    @FunctionalInterface
    public interface AdminDeleteCallback {
        void onResult(boolean success, String id, String message);
    }

    /**
     * Dữ liệu user đã parse để hiển thị trên bảng quản trị.
     *
     * @param id mã user
     * @param username tên đăng nhập
     * @param email email user
     * @param status trạng thái online/offline
     */
    public record UserSnapshot(
            String id,
            String username,
            String email,
            String status) {
    }

    /**
     * Dữ liệu auction đã parse để hiển thị trên bảng quản trị.
     *
     * @param id mã phiên
     * @param productName tên sản phẩm
     * @param seller tên người bán
     * @param currentPrice giá hiện tại dạng chuỗi
     * @param status trạng thái phiên
     */
    public record AuctionSnapshot(
            String id,
            String productName,
            String seller,
            String currentPrice,
            String status) {
    }

    /**
     * Gửi yêu cầu tải danh sách user.
     *
     * @param callback callback nhận kết quả từ server
     */
    public void fetchUsers(final AdminUsersCallback callback) {
        /*
         * Ghi nhận callback trước khi gửi command để nếu server phản hồi rất nhanh
         * thì handler vẫn có nơi trả kết quả về controller.
         */
        currentUsersCallback = callback;

        boolean sent = NetworkClient.getInstance().sendCommand(
                Protocol.Command.ADMIN_LIST_USERS.name());
        if (!sent && currentUsersCallback != null) {
            // sendCommand false nghĩa là chưa tới server, nên trả lỗi local ngay.
            currentUsersCallback.onResult(
                    false,
                    "Không gửi được yêu cầu tải người dùng tới server.",
                    List.of());
            currentUsersCallback = null;
        }
    }

    /**
     * Gửi yêu cầu tải danh sách phiên đấu giá.
     *
     * @param callback callback nhận kết quả từ server
     */
    public void fetchAuctions(final AdminAuctionsCallback callback) {
        /*
         * Danh sách auction được lấy qua server để trạng thái lifecycle và quyền
         * admin nhất quán với các command khác.
         */
        currentAuctionsCallback = callback;

        boolean sent = NetworkClient.getInstance().sendCommand(
                Protocol.Command.ADMIN_LIST_AUCTIONS.name());
        if (!sent && currentAuctionsCallback != null) {
            currentAuctionsCallback.onResult(
                    false,
                    "Không gửi được yêu cầu tải phiên đấu giá tới server.",
                    List.of());
            currentAuctionsCallback = null;
        }
    }

    /**
     * Gửi yêu cầu xóa user.
     *
     * @param userId id user cần xóa
     * @param callback callback nhận kết quả thao tác
     */
    public void deleteUser(final String userId, final AdminDeleteCallback callback) {
        /*
         * Xóa user là thao tác bất đồng bộ: controller không tự xóa dòng bảng cho
         * tới khi nhận ADMIN_DELETE_USER_OK từ server.
         */
        currentDeleteUserCallback = callback;

        boolean sent = NetworkClient.getInstance().sendCommand(
                Protocol.Command.ADMIN_DELETE_USER.name()
                        + Protocol.SEPARATOR
                        + userId);
        if (!sent && currentDeleteUserCallback != null) {
            currentDeleteUserCallback.onResult(
                    false,
                    userId,
                    "Không gửi được yêu cầu xóa người dùng tới server.");
            currentDeleteUserCallback = null;
        }
    }

    /**
     * Gửi yêu cầu xóa phiên đấu giá.
     *
     * @param auctionId id phiên cần xóa
     * @param callback callback nhận kết quả thao tác
     */
    public void deleteAuction(final String auctionId, final AdminDeleteCallback callback) {
        /*
         * Tương tự user, xóa auction chỉ cập nhật UI sau khi server xác nhận để
         * tránh bảng client lệch với dữ liệu thật.
         */
        currentDeleteAuctionCallback = callback;

        boolean sent = NetworkClient.getInstance().sendCommand(
                Protocol.Command.ADMIN_DELETE_AUCTION.name()
                        + Protocol.SEPARATOR
                        + auctionId);
        if (!sent && currentDeleteAuctionCallback != null) {
            currentDeleteAuctionCallback.onResult(
                    false,
                    auctionId,
                    "Không gửi được yêu cầu xóa phiên tới server.");
            currentDeleteAuctionCallback = null;
        }
    }

    private void handleUserList(final String response) {
        /*
         * Nếu không còn callback đang chờ, response này có thể đến muộn từ thao
         * tác cũ. Bỏ qua để không cập nhật nhầm controller hiện tại.
         */
        if (currentUsersCallback == null) {
            return;
        }

        currentUsersCallback.onResult(true, "", parseUsers(response));
        currentUsersCallback = null;
    }

    private void handleUserListFailure(final String response) {
        // Chuyển lỗi protocol thành callback chung success=false cho controller.
        if (currentUsersCallback == null) {
            return;
        }

        currentUsersCallback.onResult(false, responseMessage(response), List.of());
        currentUsersCallback = null;
    }

    private void handleAuctionList(final String response) {
        // Parse dữ liệu thô thành snapshot để controller không phải biết protocol.
        if (currentAuctionsCallback == null) {
            return;
        }

        currentAuctionsCallback.onResult(true, "", parseAuctions(response));
        currentAuctionsCallback = null;
    }

    private void handleAuctionListFailure(final String response) {
        // Giữ cùng contract với handleUserListFailure: message + list rỗng.
        if (currentAuctionsCallback == null) {
            return;
        }

        currentAuctionsCallback.onResult(false, responseMessage(response), List.of());
        currentAuctionsCallback = null;
    }

    private List<UserSnapshot> parseUsers(final String response) {
        final List<UserSnapshot> users = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        // Record đầu tiên là header ADMIN_USER_LIST|count, các record sau mới là user.
        for (int i = FIRST_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX, -1);
            if (parts.length >= MIN_USER_PARTS) {
                users.add(new UserSnapshot(
                        parts[IDX_ID],
                        parts[IDX_USER_USERNAME],
                        parts[IDX_USER_EMAIL],
                        parts[IDX_USER_STATUS]));
            }
        }

        return users;
    }

    private List<AuctionSnapshot> parseAuctions(final String response) {
        final List<AuctionSnapshot> auctions = new ArrayList<>();
        final String[] records = response.split(Protocol.RECORD_SEPARATOR);

        // Bỏ qua record thiếu field để một dòng lỗi không làm hỏng toàn bộ bảng.
        for (int i = FIRST_RECORD_INDEX; i < records.length; i++) {
            final String[] parts = records[i].split(Protocol.SEPARATOR_REGEX, -1);
            if (parts.length >= MIN_AUCTION_PARTS) {
                auctions.add(new AuctionSnapshot(
                        parts[IDX_ID],
                        parts[IDX_AUCTION_PRODUCT],
                        parts[IDX_AUCTION_SELLER],
                        parts[IDX_AUCTION_PRICE],
                        parts[IDX_AUCTION_STATUS]));
            }
        }

        return auctions;
    }

    private void handleDeleteUserOk(final String response) {
        // Response OK chứa id đã xóa, completeDelete ghép message hiển thị.
        completeDelete(currentDeleteUserCallback, true, response, "Đã xóa người dùng ");
        currentDeleteUserCallback = null;
    }

    private void handleDeleteUserFail(final String response) {
        // Response FAIL chứa message lỗi từ server.
        completeDelete(currentDeleteUserCallback, false, response, "");
        currentDeleteUserCallback = null;
    }

    private void handleDeleteAuctionOk(final String response) {
        // Response OK chứa auctionId để controller xóa đúng dòng trong bảng.
        completeDelete(currentDeleteAuctionCallback, true, response, "Đã xóa phiên ");
        currentDeleteAuctionCallback = null;
    }

    private void handleDeleteAuctionFail(final String response) {
        // Response FAIL dùng chung định dạng id/message ở field thứ hai.
        completeDelete(currentDeleteAuctionCallback, false, response, "");
        currentDeleteAuctionCallback = null;
    }

    private void completeDelete(
            final AdminDeleteCallback callback,
            final boolean success,
            final String response,
            final String successMessagePrefix) {
        if (callback == null) {
            return;
        }

        final String value = responseMessage(response);
        // Với response OK, server chỉ trả id; service ghép message thân thiện cho controller.
        final String message = success ? successMessagePrefix + value : value;
        callback.onResult(success, value, message);
    }

    private String responseMessage(final String response) {
        /*
         * Các response quản trị dạng TYPE|value. Split với -1 để giữ cả field
         * rỗng nếu server trả TYPE|, giúp fallback ở controller hoạt động.
         */
        final String[] parts = response.split(Protocol.SEPARATOR_REGEX, -1);
        return parts.length > IDX_RESPONSE_VALUE ? parts[IDX_RESPONSE_VALUE] : "";
    }
}
