package auction_system.server.network.command.auth;

import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.auth.RegisterPayload;
import auction_system.server.services.auth.AuthService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng ký tài khoản mới từ client.
 *
 * <p>Lệnh client gửi lên:
 *
 * <pre>{@code
 * JSON REGISTER chứa username, email, password và role trong payload.
 * }</pre>
 *
 * <p>Phản hồi thành công:
 *
 * <pre>{@code
 * REGISTER_OK
 * }</pre>
 *
 * <p>Phản hồi thất bại:
 *
 * <pre>{@code
 * JSON REGISTER_FAIL chứa lý do thất bại trong trường message.
 * }</pre>
 */
public class RegisterCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(RegisterCommand.class);
    private final AuthService authService;
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command đăng ký tài khoản.
     *
     * @param authService service xử lý nghiệp vụ đăng ký tài khoản
     * @param auctionManager manager dùng để phát sự kiện realtime
     */
    public RegisterCommand(
            final AuthService authService,
            final AuctionManager auctionManager) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh đăng ký tài khoản.
     *
     * @param payload payload JSON của request
     * @param session phiên làm việc hiện tại của client
     * @return phản hồi gửi về client
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        final RegisterPayload registerPayload;
        try {
            registerPayload = JsonProtocol.payloadAs(payload, RegisterPayload.class);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Không map được payload đăng ký: {}", exception.getMessage());
            return buildFailResponse("Thiếu thông tin đăng ký");
        }

        if (registerPayload.hasMissingRequiredFields()) {
            return buildFailResponse("Thiếu thông tin đăng ký");
        }

        try {
            final User registeredUser =
                    authService.register(registerPayload.toRegisterRequest());

            LOGGER.info(
                    "Đăng ký mới: "
                            + registeredUser.getUsername()
                            + " ["
                            + registeredUser.getRoleName()
                            + "]");
            // Đồng bộ user mới vào registry của AuctionManager để các luồng quản trị
            // (ví dụ xóa user theo ID) nhìn thấy ngay lập tức.
            auctionManager.registerUser(registeredUser);
            // Báo cho các màn hình quản trị đang mở biết danh sách user đã đổi.
            auctionManager.notifyUserListChanged();

            return buildSuccessResponse();
        } catch (IllegalArgumentException exception) {
            return buildFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error(
                    "Lỗi hệ thống khi xử lý đăng ký cho email: " + registerPayload.email(),
                    exception);

            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    private String buildSuccessResponse() {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.REGISTER_OK.name(),
                            null,
                            "OK",
                            null,
                            "Đăng ký tài khoản thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response đăng ký: {}", exception.getMessage());
            return Protocol.Response.REGISTER_OK.name();
        }
    }

    /**
     * Tạo phản hồi đăng ký thất bại theo đúng giao thức.
     *
     * @param message thông báo lỗi gửi về client
     * @return chuỗi phản hồi thất bại
     */
    private String buildFailResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.REGISTER_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi đăng ký: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON REGISTER_FAIL.", exception);
        }
    }
}
