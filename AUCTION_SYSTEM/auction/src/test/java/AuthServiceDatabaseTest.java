import static org.junit.jupiter.api.Assertions.*;

import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.AuthService;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử tích hợp cho {@link AuthService}: đăng ký, đăng nhập,
 * kiểm tra username/email đã tồn tại.
 *
 * <p>Mỗi test dùng {@code @TempDir} riêng để các file {@code .ser} không
 * bị dùng chung, đảm bảo các test hoàn toàn độc lập nhau.
 */
class AuthServiceDatabaseTest {

    /** Thư mục tạm do JUnit 5 tạo và dọn dẹp tự động sau mỗi test. */
    @TempDir
    Path tempDir;

    /** Cơ sở dữ liệu serialization trỏ vào {@code tempDir}. */
    private SerializedDatabase database;

    /** Đối tượng chính cần kiểm thử. */
    private AuthService authService;

    /**
     * Khởi tạo {@link SerializedDatabase} và {@link AuthService} mới
     * trước mỗi test, đảm bảo trạng thái sạch.
     */
    @BeforeEach
    void setUp() {
        database    = new SerializedDatabase(tempDir);
        authService = new AuthService(database);
    }

    // =========================================================================
    // register()
    // =========================================================================

    /**
     * Đăng ký tài khoản BIDDER hợp lệ phải trả về user với đúng username
     * và roleName.
     */
    @Test
    void register_ValidBidder_ReturnsUserWithCorrectRole() {
        User user = authService.register("bidder01", "bidder01@mail.com", "secret1", "BIDDER");

        assertNotNull(user);
        assertEquals("bidder01", user.getUsername());
        assertEquals("BIDDER", user.getRoleName());
    }

    /**
     * Đăng ký tài khoản SELLER hợp lệ phải trả về user với đúng roleName.
     */
    @Test
    void register_ValidSeller_ReturnsUserWithCorrectRole() {
        User user = authService.register("seller01", "seller01@mail.com", "secret1", "SELLER");

        assertNotNull(user);
        assertEquals("SELLER", user.getRoleName());
    }

    /**
     * Mật khẩu lưu trong database không được là plaintext.
     *
     * <p>{@code AuthService.registerInTransaction()} gọi
     * {@code SecurityUtils.hashPassword()} trước khi lưu; test này xác nhận
     * hành vi đó thực sự xảy ra.
     */
    @Test
    void register_PasswordIsHashedNotPlaintext() {
        User user = authService.register("hashtest", "hashtest@mail.com", "plainpass", "BIDDER");

        assertNotEquals("plainpass", user.getPassword(),
                "Mật khẩu không được lưu dưới dạng plaintext.");
    }

    /**
     * Mật khẩu được lưu phải khớp với kết quả của {@code SecurityUtils.hashPassword()}.
     *
     * <p>Test tường minh thuật toán hash (SHA-256), tránh trường hợp service
     * dùng thuật toán khác mà test ở trên không phát hiện được.
     */
    @Test
    void register_PasswordHashMatchesSha256() {
        String rawPassword = "plainpass";
        User user = authService.register("hashtest2", "hashtest2@mail.com", rawPassword, "BIDDER");

        assertEquals(SecurityUtils.hashPassword(rawPassword), user.getPassword(),
                "Mật khẩu phải được hash bằng SHA-256.");
    }

    /**
     * Sau khi đăng ký, user phải được ghi xuống file và đọc lại được sau
     * khi gọi {@code reloadAll()}.
     *
     * <p>Xác nhận toàn bộ chuỗi persistence: {@code register()} →
     * {@code flushAll()} → {@code reloadAll()} → {@code findByEmail()}.
     */
    @Test
    void register_UserIsPersisted_CanBeReloadedFromDatabase() {
        authService.register("persist01", "persist01@mail.com", "abc123", "BIDDER");

        database.reloadAll();

        Optional<User> found = database.users().findByEmail("persist01@mail.com");
        assertTrue(found.isPresent(), "Người dùng phải được ghi xuống file .ser.");
        assertEquals("persist01", found.get().getUsername());
    }

    /**
     * File {@code users.ser} phải tồn tại trên đĩa sau khi đăng ký thành công.
     *
     * <p>Test tầng file system: nếu {@code flushAll()} bị bỏ quên trong
     * service, file sẽ không được tạo và test này thất bại.
     */
    @Test
    void register_SerFileExists_AfterRegistration() {
        authService.register("filecheck", "filecheck@mail.com", "abc123", "BIDDER");

        assertTrue(Files.exists(tempDir.resolve("users.ser")),
                "File users.ser phải được tạo sau khi đăng ký.");
    }

    /**
     * Email không có ký tự {@code @} phải bị từ chối ngay trong
     * {@code validateRegisterRequest()}.
     *
     * <p>{@code isValidEmail()} yêu cầu {@code atIndex > 0}; email không có
     * {@code @} cho {@code atIndex = -1}, vi phạm điều kiện này.
     */
    @Test
    void register_InvalidEmail_MissingAt_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u1", "notanemail", "abc123", "BIDDER"),
                "Email không có @ phải ném IllegalArgumentException.");
    }

    /**
     * Email không có dấu chấm sau {@code @} phải bị từ chối.
     *
     * <p>{@code isValidEmail()} yêu cầu {@code dotIndex > atIndex + 1};
     * với {@code "user@nodot"}, {@code lastIndexOf('.')} trả về {@code -1},
     * vi phạm điều kiện này.
     */
    @Test
    void register_InvalidEmail_NoDomainDot_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u2", "user@nodot", "abc123", "BIDDER"),
                "Email thiếu dấu chấm sau @ phải ném IllegalArgumentException.");
    }

    /**
     * Mật khẩu dưới 6 ký tự phải bị từ chối.
     *
     * <p>{@code validateRegisterRequest()} so sánh {@code password.length()}
     * với hằng số {@code minimumPasswordLength = 6}; chuỗi 5 ký tự vi phạm
     * điều kiện này.
     */
    @Test
    void register_PasswordTooShort_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u3", "u3@mail.com", "12345", "BIDDER"),
                "Mật khẩu dưới 6 ký tự phải ném IllegalArgumentException.");
    }

    /**
     * Mật khẩu đúng 6 ký tự phải được chấp nhận — kiểm tra biên dưới
     * của {@code minimumPasswordLength}.
     *
     * <p>Bổ sung cạnh còn thiếu: test trên chỉ kiểm tra trường hợp vi phạm
     * (5 ký tự), test này xác nhận biên đúng (6 ký tự) không ném ngoại lệ.
     */
    @Test
    void register_PasswordExactlyMinLength_DoesNotThrow() {
        assertDoesNotThrow(
                () -> authService.register("u3b", "u3b@mail.com", "123456", "BIDDER"),
                "Mật khẩu đúng 6 ký tự phải được chấp nhận.");
    }

    /**
     * Đăng ký trùng username phải bị từ chối dù email khác nhau.
     *
     * <p>{@code registerInTransaction()} kiểm tra {@code existsByUsername()}
     * trước khi lưu; vi phạm ném {@link IllegalArgumentException}.
     */
    @Test
    void register_DuplicateUsername_ThrowsException() {
        authService.register("dup", "dup1@mail.com", "abc123", "BIDDER");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("dup", "dup2@mail.com", "abc123", "BIDDER"),
                "Username trùng phải ném IllegalArgumentException.");
    }

    /**
     * Đăng ký trùng email phải bị từ chối dù username khác nhau.
     *
     * <p>{@code registerInTransaction()} kiểm tra {@code existsByEmail()}
     * sau khi đã qua kiểm tra username; vi phạm ném {@link IllegalArgumentException}.
     */
    @Test
    void register_DuplicateEmail_ThrowsException() {
        authService.register("user_a", "dupe@mail.com", "abc123", "BIDDER");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_b", "dupe@mail.com", "abc123", "BIDDER"),
                "Email trùng phải ném IllegalArgumentException.");
    }

    /**
     * Đăng ký với email trùng nhưng khác hoa/thường phải bị từ chối.
     *
     * <p>{@code UserRepository.findByEmail()} dùng {@code equalsIgnoreCase()},
     * nên {@code "DUP@MAIL.COM"} và {@code "dup@mail.com"} được coi là một.
     * Test này xác nhận behavior đó được duy trì qua toàn bộ chuỗi service.
     */
    @Test
    void register_DuplicateEmail_CaseInsensitive_ThrowsException() {
        authService.register("user_a", "dup@mail.com", "abc123", "BIDDER");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_b", "DUP@MAIL.COM", "abc123", "BIDDER"),
                "Email trùng theo case-insensitive phải ném IllegalArgumentException.");
    }

    /**
     * Vai trò {@code ADMIN} không được phép đăng ký qua service.
     *
     * <p>{@code validateRoleName()} chỉ chấp nhận {@code BIDDER} và
     * {@code SELLER}; bất kỳ giá trị nào khác (kể cả {@code ADMIN}) đều
     * ném {@link IllegalArgumentException}.
     */
    @Test
    void register_InvalidRole_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u4", "u4@mail.com", "abc123", "ADMIN"),
                "Vai trò không hợp lệ (ADMIN) phải ném IllegalArgumentException.");
    }

    /**
     * Username rỗng ({@code ""}) phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void register_EmptyUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "u5@mail.com", "abc123", "BIDDER"));
    }

    /**
     * Username chỉ có khoảng trắng phải bị từ chối.
     *
     * <p>{@code validateText()} dùng {@code isBlank()} — bắt cả chuỗi rỗng
     * lẫn chuỗi chỉ có whitespace. Test bổ sung cạnh còn thiếu so với
     * {@code register_EmptyUsername_ThrowsException}.
     */
    @Test
    void register_BlankUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("   ", "u5b@mail.com", "abc123", "BIDDER"));
    }

    /**
     * Password {@code null} phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void register_NullPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u6", "u6@mail.com", null, "BIDDER"));
    }

    /**
     * Username {@code null} phải bị từ chối bởi {@code validateText()}.
     *
     * <p>Đối xứng với {@code register_EmptyUsername_ThrowsException} và
     * {@code register_BlankUsername_ThrowsException} — đảm bảo mọi dạng
     * "rỗng" đều bị bắt.
     */
    @Test
    void register_NullUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null, "u7@mail.com", "abc123", "BIDDER"));
    }

    /**
     * Email {@code null} phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void register_NullEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u8", null, "abc123", "BIDDER"));
    }

    /**
     * Role {@code null} phải bị từ chối bởi {@code validateText()} trước
     * khi chạm tới {@code validateRoleName()}.
     */
    @Test
    void register_NullRole_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u9", "u9@mail.com", "abc123", null));
    }

    // =========================================================================
    // login()
    // =========================================================================

    /**
     * Đăng nhập với đúng email và mật khẩu phải trả về user tương ứng.
     */
    @Test
    void login_CorrectCredentials_ReturnsUser() {
        authService.register("loginUser", "login@mail.com", "mypass1", "BIDDER");

        Optional<User> result = authService.login("login@mail.com", "mypass1");

        assertTrue(result.isPresent(), "Đăng nhập đúng credentials phải trả về user.");
        assertEquals("loginUser", result.get().getUsername());
    }

    /**
     * Đăng nhập với email hợp lệ nhưng sai mật khẩu phải trả về
     * {@code Optional.empty()}.
     *
     * <p>{@code isPasswordMatched()} so sánh hash của input với hash đã lưu;
     * không khớp → trả về rỗng, không ném ngoại lệ.
     */
    @Test
    void login_WrongPassword_ReturnsEmpty() {
        authService.register("loginUser2", "login2@mail.com", "correctPass", "BIDDER");

        Optional<User> result = authService.login("login2@mail.com", "wrongPass");

        assertTrue(result.isEmpty(), "Sai mật khẩu phải trả về Optional.empty().");
    }

    /**
     * Đăng nhập với email không tồn tại phải trả về {@code Optional.empty()}.
     *
     * <p>{@code findValidUser()} gọi {@code findByEmail()} trả về rỗng →
     * trả về {@code Optional.empty()} ngay, không kiểm tra mật khẩu.
     */
    @Test
    void login_UnknownEmail_ReturnsEmpty() {
        Optional<User> result = authService.login("ghost@mail.com", "abc123");

        assertTrue(result.isEmpty(), "Email không tồn tại phải trả về Optional.empty().");
    }

    /**
     * Email {@code null} phải bị từ chối bởi {@code validateText()} trước
     * khi truy vấn database.
     */
    @Test
    void login_NullEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "abc123"),
                "Email null phải ném IllegalArgumentException.");
    }

    /**
     * Email chỉ chứa khoảng trắng phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void login_BlankEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("   ", "abc123"),
                "Email rỗng phải ném IllegalArgumentException.");
    }

    /**
     * Password {@code null} phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void login_NullPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", null),
                "Password null phải ném IllegalArgumentException.");
    }

    /**
     * Password rỗng phải bị từ chối bởi {@code validateText()}.
     *
     * <p>{@code isBlank("")} trả về {@code true} → ném trước khi so sánh hash.
     */
    @Test
    void login_BlankPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", ""),
                "Password rỗng phải ném IllegalArgumentException.");
    }

    /**
     * Đăng nhập với email khác hoa/thường so với lúc đăng ký vẫn phải thành công.
     *
     * <p>{@code UserRepository.findByEmail()} dùng {@code equalsIgnoreCase()};
     * test xác nhận hành vi này được duy trì xuyên suốt chuỗi
     * {@code login()} → {@code findByEmail()}.
     */
    @Test
    void login_EmailCaseInsensitive_ReturnsUser() {
        authService.register("caseUser", "case@mail.com", "mypass1", "BIDDER");

        Optional<User> result = authService.login("CASE@MAIL.COM", "mypass1");

        assertTrue(result.isPresent(), "Đăng nhập với email khác hoa/thường phải thành công.");
    }

    /**
     * Đăng nhập với mật khẩu plaintext cũ phải thành công, đồng thời service
     * phải tự động nâng cấp mật khẩu sang dạng hash và ghi lại xuống database.
     *
     * <p>Kịch bản: dữ liệu cũ từ trước khi hệ thống áp dụng hash có mật khẩu
     * lưu dạng plaintext. {@code upgradeLegacyPasswordIfNeeded()} phát hiện
     * điều này qua so sánh {@code rawPassword.equals(storedPassword)} và tự
     * động nâng cấp sau khi đăng nhập thành công.
     */
    @Test
    void login_LegacyPlaintextPassword_LoginSucceeds() {
        authService.register("legacy", "legacy@mail.com", "legacyPass", "BIDDER");

        User user = database.users().findByEmail("legacy@mail.com").orElseThrow();
        user.setPassword("legacyPass");
        database.users().save(user);
        database.flushAll();

        Optional<User> result = authService.login("legacy@mail.com", "legacyPass");

        assertTrue(result.isPresent(), "Đăng nhập với mật khẩu plaintext cũ phải thành công.");
    }

    /**
     * Sau khi đăng nhập với mật khẩu plaintext cũ, mật khẩu trong database
     * phải được ghi đè bằng bản hash SHA-256.
     *
     * <p>Tách khỏi {@code login_LegacyPlaintextPassword_LoginSucceeds} để mỗi
     * test chỉ assert một điều — dễ xác định nguyên nhân khi thất bại.
     */
    @Test
    void login_LegacyPlaintextPassword_UpgradedToHashInDatabase() {
        authService.register("legacy2", "legacy2@mail.com", "legacyPass", "BIDDER");

        User user = database.users().findByEmail("legacy2@mail.com").orElseThrow();
        user.setPassword("legacyPass");
        database.users().save(user);
        database.flushAll();

        authService.login("legacy2@mail.com", "legacyPass");

        database.reloadAll();
        User reloaded = database.users().findByEmail("legacy2@mail.com").orElseThrow();
        assertEquals(SecurityUtils.hashPassword("legacyPass"), reloaded.getPassword(),
                "Mật khẩu phải được nâng cấp sang hash sau đăng nhập.");
    }

    // =========================================================================
    // isUsernameTaken() / isEmailTaken()
    // =========================================================================

    /**
     * Username đã đăng ký phải được nhận diện là đã tồn tại.
     */
    @Test
    void isUsernameTaken_ExistingUsername_ReturnsTrue() {
        authService.register("takenUser", "taken@mail.com", "abc123", "BIDDER");

        assertTrue(authService.isUsernameTaken("takenUser"));
    }

    /**
     * Username chưa có trong database phải trả về {@code false}.
     */
    @Test
    void isUsernameTaken_NewUsername_ReturnsFalse() {
        assertFalse(authService.isUsernameTaken("brandnewuser"));
    }

    /**
     * Username {@code null} phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void isUsernameTaken_NullUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isUsernameTaken(null));
    }

    /**
     * Username chỉ có khoảng trắng phải bị từ chối bởi {@code validateText()}.
     *
     * <p>Đối xứng với {@code isUsernameTaken_NullUsername_ThrowsException} —
     * {@code isBlank()} bắt cả hai trường hợp.
     */
    @Test
    void isUsernameTaken_BlankUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isUsernameTaken("   "));
    }

    /**
     * Email đã đăng ký phải được nhận diện là đã tồn tại.
     */
    @Test
    void isEmailTaken_ExistingEmail_ReturnsTrue() {
        authService.register("emailUser", "taken_email@mail.com", "abc123", "SELLER");

        assertTrue(authService.isEmailTaken("taken_email@mail.com"));
    }

    /**
     * Email chưa có trong database phải trả về {@code false}.
     */
    @Test
    void isEmailTaken_NewEmail_ReturnsFalse() {
        assertFalse(authService.isEmailTaken("new_email@mail.com"));
    }

    /**
     * Email chỉ có khoảng trắng phải bị từ chối bởi {@code validateText()}.
     */
    @Test
    void isEmailTaken_BlankEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isEmailTaken("  "));
    }

    /**
     * Email {@code null} phải bị từ chối bởi {@code validateText()}.
     *
     * <p>Đối xứng với {@code isEmailTaken_BlankEmail_ThrowsException} —
     * đảm bảo mọi dạng "rỗng" đều được bắt.
     */
    @Test
    void isEmailTaken_NullEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isEmailTaken(null));
    }
}