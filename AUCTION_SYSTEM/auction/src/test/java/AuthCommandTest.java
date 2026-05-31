import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.Admin;
import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.auth.LoginCommand;
import auction_system.server.network.command.auth.LogoutCommand;
import auction_system.server.network.command.auth.RegisterCommand;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.auth.request.LoginRequest;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử các command xác thực: {@link LoginCommand}, {@link RegisterCommand},
 * {@link LogoutCommand}.
 *
 * <p>Các nhóm test:
 * <ol>
 *   <li>LoginCommand — chưa đăng nhập, payload thiếu, sai thông tin, thành công,
 *       tài khoản đang online.</li>
 *   <li>RegisterCommand — payload thiếu, email trùng, thành công.</li>
 *   <li>LogoutCommand — chưa đăng nhập, đã đăng nhập.</li>
 * </ol>
 */
public class AuthCommandTest {

    @TempDir
    Path tempDir;

    private AuctionManager manager;
    private AuthService authService;
    private SerializedDatabase database;

    /** Observer không làm gì, dùng để dựng ClientSession trong test. */
    private static final AuctionObserver NOOP_OBSERVER = msg -> { };

    @BeforeEach
    void setUp() throws Exception {
        resetSingleton();
        database = new SerializedDatabase(tempDir);
        manager = AuctionManager.getInstance(database);
        authService = new AuthService(database);
    }

    @AfterEach
    void tearDown() throws Exception {
        manager.shutdown();
        resetSingleton();
    }

    /** Reset AuctionManager singleton để các test độc lập nhau. */
    private void resetSingleton() throws Exception {
        Field field = AuctionManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    /** Tạo ClientSession mới chưa đăng nhập. */
    private ClientSession newSession() {
        return new ClientSession(NOOP_OBSERVER, manager);
    }

    /**
     * Tạo ClientSession đã đăng nhập với user cho trước.
     *
     * @param user người dùng đã đăng nhập
     * @return session đã đặt currentUser
     */
    private ClientSession loggedInSession(final User user) {
        ClientSession session = newSession();
        session.setCurrentUser(user);
        return session;
    }

    /** Tạo Participant mới dùng trong test. */
    private Participant makeParticipant() {
        return new Participant("user01", "user01@test.com", "pass123", 0.0, "PARTICIPANT");
    }

    /**
     * Tạo JsonNode payload đăng nhập.
     *
     * @param email địa chỉ email
     * @param password mật khẩu
     * @return payload JSON
     */
    private JsonNode loginPayload(final String email, final String password) {
        return JsonProtocol.payloadOf(Map.of("email", email, "password", password));
    }

    /**
     * Tạo JsonNode payload đăng ký tài khoản.
     *
     * @param username tên đăng nhập
     * @param email địa chỉ email
     * @param password mật khẩu
     * @param role tên vai trò
     * @return payload JSON
     */
    private JsonNode registerPayload(
            final String username,
            final String email,
            final String password,
            final String role) {
        return JsonProtocol.payloadOf(
                Map.of("username", username, "email", email,
                        "password", password, "roleName", role));
    }

    // =========================================================================
    // LoginCommand — payload lỗi / thiếu
    // =========================================================================

    /**
     * Login với payload null phải trả về LOGIN_FAIL.
     */
    @Test
    void loginCommand_NullPayload_ReturnsLoginFail() {
        LoginCommand cmd = new LoginCommand(authService, manager);
        ClientSession session = newSession();

        String response = cmd.execute(null, session);

        assertNotNull(response, "Response khong duoc null.");
        assertTrue(response.contains(Protocol.Response.LOGIN_FAIL.name()),
                "Payload null phai tra LOGIN_FAIL.");
    }

    /**
     * Login thiếu password phải trả về LOGIN_FAIL.
     */
    @Test
    void loginCommand_MissingPassword_ReturnsLoginFail() {
        LoginCommand cmd = new LoginCommand(authService, manager);
        ClientSession session = newSession();
        JsonNode payload = JsonProtocol.payloadOf(Map.of("email", "x@test.com"));

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.LOGIN_FAIL.name()),
                "Thieu password phai tra LOGIN_FAIL.");
    }

    /**
     * Login thiếu email phải trả về LOGIN_FAIL.
     */
    @Test
    void loginCommand_BlankEmail_ReturnsLoginFail() {
        LoginCommand cmd = new LoginCommand(authService, manager);
        ClientSession session = newSession();
        JsonNode payload = JsonProtocol.payloadOf(Map.of("email", "   ", "password", "abc"));

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.LOGIN_FAIL.name()),
                "Email blank phai tra LOGIN_FAIL.");
    }

    /**
     * Login với email không tồn tại phải trả về LOGIN_FAIL.
     */
    @Test
    void loginCommand_WrongEmail_ReturnsLoginFail() {
        LoginCommand cmd = new LoginCommand(authService, manager);
        ClientSession session = newSession();
        JsonNode payload = loginPayload("notexist@test.com", "wrongpass");

        String response = cmd.execute(payload, session);

        assertTrue(response.contains(Protocol.Response.LOGIN_FAIL.name()),
                "Email sai phai tra LOGIN_FAIL.");
    }

    // =========================================================================
    // LoginCommand — tài khoản đang online
    // =========================================================================

    /**
     * Login khi tài khoản đang online phải trả về LOGIN_FAIL.
     */
    @Test
    void loginCommand_AlreadyOnline_ReturnsLoginFail() throws Exception {
        // Đăng ký tài khoản trước
        RegisterCommand reg = new RegisterCommand(authService, manager);
        JsonNode regPayload = registerPayload("onlineuser", "online@test.com", "pass123", "PARTICIPANT");
        reg.execute(regPayload, newSession());

        // Đăng nhập lần 1 — đặt user vào registry online
        Optional<User> userOpt = authService.login(
                new LoginRequest(
                        "online@test.com", "pass123"));
        assertTrue(userOpt.isPresent(), "Dang nhap lan 1 phai thanh cong.");
        manager.userLoggedIn(userOpt.get(), NOOP_OBSERVER);

        // Đăng nhập lần 2 — phải bị từ chối
        LoginCommand cmd = new LoginCommand(authService, manager);
        JsonNode payload = loginPayload("online@test.com", "pass123");
        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.LOGIN_FAIL.name()),
                "Tai khoan dang online phai tra LOGIN_FAIL.");
    }

    // =========================================================================
    // RegisterCommand — payload lỗi
    // =========================================================================

    /**
     * Register với payload null phải trả về REGISTER_FAIL.
     */
    @Test
    void registerCommand_NullPayload_ReturnsRegisterFail() {
        RegisterCommand cmd = new RegisterCommand(authService, manager);

        String response = cmd.execute(null, newSession());

        assertTrue(response.contains(Protocol.Response.REGISTER_FAIL.name()),
                "Payload null phai tra REGISTER_FAIL.");
    }

    /**
     * Register thiếu username phải trả về REGISTER_FAIL.
     */
    @Test
    void registerCommand_BlankUsername_ReturnsRegisterFail() {
        RegisterCommand cmd = new RegisterCommand(authService, manager);
        JsonNode payload = registerPayload("   ", "a@b.com", "pass123", "PARTICIPANT");

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.REGISTER_FAIL.name()),
                "Username blank phai tra REGISTER_FAIL.");
    }

    /**
     * Register thiếu roleName phải trả về REGISTER_FAIL.
     */
    @Test
    void registerCommand_BlankRoleName_ReturnsRegisterFail() {
        RegisterCommand cmd = new RegisterCommand(authService, manager);
        JsonNode payload = registerPayload("user2", "user2@test.com", "pass123", "   ");

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.REGISTER_FAIL.name()),
                "RoleName blank phai tra REGISTER_FAIL.");
    }

    /**
     * Register thành công phải trả về REGISTER_OK.
     */
    @Test
    void registerCommand_ValidPayload_ReturnsRegisterOk() {
        RegisterCommand cmd = new RegisterCommand(authService, manager);
        JsonNode payload = registerPayload("newuser", "new@test.com", "pass123", "PARTICIPANT");

        String response = cmd.execute(payload, newSession());

        assertTrue(response.contains(Protocol.Response.REGISTER_OK.name()),
                "Dang ky hop le phai tra REGISTER_OK.");
    }

    /**
     * Register hai lần cùng email phải trả về REGISTER_FAIL lần hai.
     */
    @Test
    void registerCommand_DuplicateEmail_ReturnsRegisterFail() {
        RegisterCommand cmd = new RegisterCommand(authService, manager);
        JsonNode payload = registerPayload("dup", "dup@test.com", "pass123", "PARTICIPANT");
        cmd.execute(payload, newSession());

        JsonNode payload2 = registerPayload("dup2", "dup@test.com", "pass456", "PARTICIPANT");
        String response = cmd.execute(payload2, newSession());

        assertTrue(response.contains(Protocol.Response.REGISTER_FAIL.name()),
                "Email trung phai tra REGISTER_FAIL.");
    }

    // =========================================================================
    // LogoutCommand — chưa đăng nhập
    // =========================================================================

    /**
     * Logout khi chưa đăng nhập phải trả về LOGOUT_OK (không lỗi).
     */
    @Test
    void logoutCommand_NotLoggedIn_ReturnsLogoutOk() {
        LogoutCommand cmd = new LogoutCommand(manager);
        ClientSession session = newSession();

        String response = cmd.execute(null, session);

        assertTrue(response.contains(Protocol.Response.LOGOUT_OK.name()),
                "Logout khi chua dang nhap phai tra LOGOUT_OK.");
    }

    /**
     * Logout khi đã đăng nhập phải trả về LOGOUT_OK và xoá user khỏi session.
     */
    @Test
    void logoutCommand_LoggedIn_ReturnsLogoutOkAndClearsSession() {
        LogoutCommand cmd = new LogoutCommand(manager);
        Participant user = makeParticipant();
        manager.userLoggedIn(user, NOOP_OBSERVER);
        ClientSession session = loggedInSession(user);

        String response = cmd.execute(null, session);

        assertTrue(response.contains(Protocol.Response.LOGOUT_OK.name()),
                "Logout khi da dang nhap phai tra LOGOUT_OK.");
    }

    /**
     * Sau khi logout, session không còn đăng nhập.
     */
    @Test
    void logoutCommand_AfterLogout_SessionNotLoggedIn() {
        LogoutCommand cmd = new LogoutCommand(manager);
        Participant user = makeParticipant();
        manager.userLoggedIn(user, NOOP_OBSERVER);
        ClientSession session = loggedInSession(user);

        cmd.execute(null, session);

        assertTrue(!session.isLoggedIn(),
                "Session phai khong con dang nhap sau khi logout.");
    }
}