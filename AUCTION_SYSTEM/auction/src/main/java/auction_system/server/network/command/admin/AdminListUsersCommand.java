package auction_system.server.network.command.admin;

import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command cho phép ADMIN lấy danh sách người dùng qua socket.
 */
public class AdminListUsersCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminListUsersCommand.class);
    private final AuctionManager auctionManager;

    public AdminListUsersCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        /*
         * Danh sách user là dữ liệu quản trị nên command kiểm tra quyền ở server.
         * Controller client không thể tự quyết định quyền chỉ bằng màn hình đang mở.
         */
        if (!isAdmin(session)) {
            return buildFailureResponse("Bạn không có quyền quản trị.");
        }

        final List<User> users = auctionManager.getAllUsers();
        return buildSuccessResponse(users);
    }

    private String buildSuccessResponse(final List<User> users) {
        final List<List<String>> userRecords = new ArrayList<>();
        for (User user : users) {
            userRecords.add(toUserRecord(user));
        }

        // Trả danh sách user admin bằng JSON cho dashboard quản trị.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ADMIN_USER_LIST.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "count", userRecords.size(),
                                    "users", userRecords)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response danh sách user admin: {}",
                    exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ADMIN_USER_LIST.", exception);
        }
    }

    private List<String> toUserRecord(final User user) {
        // Giữ thứ tự field user giống protocol cũ để parser dashboard không đổi contract.
        return List.of(
                String.valueOf(user.getId()),
                String.valueOf(user.getUsername()),
                String.valueOf(user.getEmail()),
                user.isOnline() ? "ONLINE" : "OFFLINE",
                String.valueOf(user.getRoleName()));
    }

    private String buildFailureResponse(final String message) {
        // Trả lỗi quyền/tải user bằng JSON để client đọc message thống nhất.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ADMIN_USER_LIST_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi danh sách user admin: {}",
                    exception.getMessage());
            throw new IllegalStateException(
                    "Không tạo được JSON ADMIN_USER_LIST_FAIL.",
                    exception);
        }
    }

    private boolean isAdmin(final ClientSession session) {
        // Chỉ user đã đăng nhập trong session và có role ADMIN mới được đọc danh sách.
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }
}
