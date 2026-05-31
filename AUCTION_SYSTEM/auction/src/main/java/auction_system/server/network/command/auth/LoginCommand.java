package auction_system.server.network.command.auth;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.payload.auth.LoginPayload;
import auction_system.server.services.auth.AuthService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý lệnh đăng nhập từ client.
 *
 * <p>Command này chỉ đọc request, gọi AuthService để xác thực bằng database,
 * sau đó cập nhật trạng thái online thông qua AuctionManager.
 */
public class LoginCommand implements JsonPayloadCommand {
    private static final Logger LOGGER = LoggerFactory.getLogger(LoginCommand.class);

    private final AuthService authService;
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command đăng nhập.
     *
     * @param authService service xác thực làm việc với database
     * @param auctionManager manager theo dõi trạng thái online
     */
    public LoginCommand(
            final AuthService authService,
            final AuctionManager auctionManager) {
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Thực thi lệnh đăng nhập.
     *
     * @param payload payload JSON {@code LOGIN}
     * @param session phiên làm việc của client hiện tại
     * @return phản hồi gửi về client
     */
    @Override
    public String execute(final JsonNode payload, final ClientSession session) {
        final LoginPayload loginPayload;
        try {
            loginPayload = JsonProtocol.payloadAs(payload, LoginPayload.class);
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Không map được payload đăng nhập: {}", exception.getMessage());
            return buildFailResponse("Thiếu thông tin đăng nhập.");
        }

        if (loginPayload.hasMissingRequiredFields()) {
            LOGGER.warn("Từ chối đăng nhập vì request thiếu tham số.");
            return buildFailResponse("Thiếu thông tin đăng nhập.");
        }

        final String email = loginPayload.email().trim();

        try {
            final Optional<User> authenticatedUser =
                    authService.login(loginPayload.toLoginRequest());
            if (authenticatedUser.isEmpty()) {
                return buildFailResponse("Email hoặc mật khẩu không đúng.");
            }

            final User user = authenticatedUser.get();
            if (auctionManager.isAlreadyOnline(user.getId())) {
                return buildFailResponse("Tài khoản này đang đăng nhập ở nơi khác.");
            }

            session.setCurrentUser(user);
            user.setOnline(true);
            auctionManager.userLoggedIn(user, session.getObserver());

            LOGGER.info("Đăng nhập thành công: {} [{}]", email, user.getRoleName());
            return buildSuccessResponse(user);
        } catch (IllegalArgumentException exception) {
            return buildFailResponse(exception.getMessage());
        } catch (RuntimeException exception) {
            LOGGER.error("Lỗi hệ thống khi đăng nhập: {}", email, exception);
            return buildFailResponse("Lỗi máy chủ nội bộ. Vui lòng thử lại sau.");
        }
    }

    /**
     * Tạo phản hồi đăng nhập thành công.
     *
     * @param user người dùng đã xác thực
     * @return chuỗi phản hồi thành công
     */
    private String buildSuccessResponse(final User user) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.LOGIN_OK.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "userId", user.getId(),
                                    "username", user.getUsername(),
                                    "email", user.getEmail(),
                                    "roleName", user.getRoleName(),
                                    "balance", getBalance(user))),
                            "Đăng nhập thành công."));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response đăng nhập: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON LOGIN_OK.", exception);
        }
    }

    /**
     * Lấy số dư của người dùng nếu tài khoản có ví.
     *
     * @param user người dùng đã đăng nhập
     * @return số dư hiện tại, hoặc 0 nếu là tài khoản không có ví
     */
    private double getBalance(final User user) {
        if (user instanceof Participant participant) {
            return participant.getBalance();
        }

        return 0;
    }

    /**
     * Tạo phản hồi đăng nhập thất bại.
     *
     * @param message thông báo lỗi
     * @return chuỗi phản hồi thất bại
     */
    private String buildFailResponse(final String message) {
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.LOGIN_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi đăng nhập: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON LOGIN_FAIL.", exception);
        }
    }
}
