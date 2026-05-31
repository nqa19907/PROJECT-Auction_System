package auction_system.client.controllers.auction;

import auction_system.client.network.NetworkClient;
import auction_system.common.models.users.Admin;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service socket cho các thao tác của màn hình Admin Dashboard.
 */
final class AdminDashboardService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminDashboardService.class);
    private final AdminDashboardResponseParser parser = new AdminDashboardResponseParser();

    private UserListCallback userListCallback;
    private AuctionListCallback auctionListCallback;
    private DeleteCallback deleteUserCallback;
    private DeleteCallback deleteAuctionCallback;
    private FailureCallback failureCallback;

    AdminDashboardService() {
        registerResponseHandlers();
    }

    @FunctionalInterface
    interface UserListCallback {
        void onResult(List<AdminUserRow> rows);
    }

    @FunctionalInterface
    interface AuctionListCallback {
        void onResult(List<AdminAuctionRow> rows);
    }

    @FunctionalInterface
    interface DeleteCallback {
        void onResult(String deletedId);
    }

    @FunctionalInterface
    interface FailureCallback {
        void onFailure(String message);
    }

    void setUserListCallback(final UserListCallback callback) {
        this.userListCallback = callback;
    }

    void setAuctionListCallback(final AuctionListCallback callback) {
        this.auctionListCallback = callback;
    }

    void setDeleteUserCallback(final DeleteCallback callback) {
        this.deleteUserCallback = callback;
    }

    void setDeleteAuctionCallback(final DeleteCallback callback) {
        this.deleteAuctionCallback = callback;
    }

    void setFailureCallback(final FailureCallback callback) {
        this.failureCallback = callback;
    }

    boolean fetchUsers() {
        return NetworkClient.getInstance()
                .sendMessage(JsonProtocol.request(Protocol.Command.ADMIN_LIST_USERS));
    }

    boolean fetchAuctions() {
        return NetworkClient.getInstance()
                .sendMessage(JsonProtocol.request(Protocol.Command.ADMIN_LIST_AUCTIONS));
    }

    boolean deleteUser(final Admin admin, final String userId) {
        return NetworkClient.getInstance().sendMessage(admin.deleteUser(userId));
    }

    boolean deleteAuction(final Admin admin, final String auctionId) {
        return NetworkClient.getInstance().sendMessage(admin.deleteAuction(auctionId));
    }

    private void registerResponseHandlers() {
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_USER_LIST.name(), this::handleUserList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_USER_LIST_FAIL.name(), this::handleUserListFail);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_AUCTION_LIST.name(), this::handleAuctionList);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name(), this::handleAuctionListFail);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_OK.name(), this::handleDeleteUserOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_USER_FAIL.name(), this::handleDeleteUserFail);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_OK.name(), this::handleDeleteAuctionOk);
        NetworkClient.getInstance().registerHandler(
                Protocol.Response.ADMIN_DELETE_AUCTION_FAIL.name(), this::handleDeleteAuctionFail);
    }

    private void handleUserList(final String response) {
        if (userListCallback == null) {
            return;
        }

        // Parser chuẩn hóa response socket thành row model trước khi controller cập nhật UI.
        userListCallback.onResult(parser.parseUsers(response));
    }

    private void handleUserListFail(final String response) {
        notifyFailure(parser.parseFailureMessage(
                response,
                "Tải danh sách người dùng thất bại."));
    }

    private void handleAuctionList(final String response) {
        if (auctionListCallback == null) {
            return;
        }

        // Controller chỉ nhận danh sách row hợp lệ, không cần biết format protocol.
        auctionListCallback.onResult(parser.parseAuctions(response));
    }

    private void handleAuctionListFail(final String response) {
        notifyFailure(parser.parseFailureMessage(
                response,
                "Tải danh sách phiên đấu giá thất bại."));
    }

    private void handleDeleteUserOk(final String response) {
        if (deleteUserCallback == null) {
            return;
        }

        // Server trả lại userId đã xóa để UI loại đúng dòng khỏi bảng hiện tại.
        final String userId = parser.parseDeletedId(response);
        if (!userId.isBlank()) {
            deleteUserCallback.onResult(userId);
        }
    }

    private void handleDeleteUserFail(final String response) {
        notifyFailure(parser.parseFailureMessage(response, "Xóa người dùng thất bại."));
    }

    private void handleDeleteAuctionOk(final String response) {
        if (deleteAuctionCallback == null) {
            return;
        }

        // Server trả lại auctionId đã xóa để UI loại đúng dòng khỏi bảng hiện tại.
        final String auctionId = parser.parseDeletedId(response);
        if (!auctionId.isBlank()) {
            deleteAuctionCallback.onResult(auctionId);
        }
    }

    private void handleDeleteAuctionFail(final String response) {
        notifyFailure(parser.parseFailureMessage(response, "Xóa phiên thất bại."));
    }

    private void notifyFailure(final String message) {
        if (failureCallback != null) {
            failureCallback.onFailure(message);
        }
    }

}
