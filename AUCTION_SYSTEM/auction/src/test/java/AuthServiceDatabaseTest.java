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
 * Kiểm thử AuthService: register, login, isUsernameTaken, isEmailTaken.
 */
class AuthServiceDatabaseTest {

    @TempDir
    Path tempDir;

    private SerializedDatabase database;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        database    = new SerializedDatabase(tempDir);
        authService = new AuthService(database);
    }

    // =========================================================================
    // register()
    // =========================================================================

    @Test
    void register_ValidBidder_ReturnsUserWithCorrectRole() {
        User user = authService.register("bidder01", "bidder01@mail.com", "secret1", "BIDDER");

        assertNotNull(user);
        assertEquals("bidder01", user.getUsername());
        assertEquals("BIDDER", user.getRoleName());
    }

    @Test
    void register_ValidSeller_ReturnsUserWithCorrectRole() {
        User user = authService.register("seller01", "seller01@mail.com", "secret1", "SELLER");

        assertNotNull(user);
        assertEquals("SELLER", user.getRoleName());
    }

    @Test
    void register_PasswordIsHashedNotPlaintext() {
        User user = authService.register("hashtest", "hashtest@mail.com", "plainpass", "BIDDER");

        assertNotEquals("plainpass", user.getPassword(),
                "Mật khẩu không được lưu dưới dạng plaintext.");
    }

    @Test
    void register_PasswordHashMatchesSha256() {
        String rawPassword = "plainpass";
        User user = authService.register("hashtest2", "hashtest2@mail.com", rawPassword, "BIDDER");

        assertEquals(SecurityUtils.hashPassword(rawPassword), user.getPassword(),
                "Mật khẩu phải được hash bằng SHA-256.");
    }

    @Test
    void register_UserIsPersisted_CanBeReloadedFromDatabase() {
        authService.register("persist01", "persist01@mail.com", "abc123", "BIDDER");

        database.reloadAll();

        Optional<User> found = database.users().findByEmail("persist01@mail.com");
        assertTrue(found.isPresent(), "Người dùng phải được ghi xuống file .ser.");
        assertEquals("persist01", found.get().getUsername());
    }

    @Test
    void register_SerFileExists_AfterRegistration() {
        authService.register("filecheck", "filecheck@mail.com", "abc123", "BIDDER");

        assertTrue(Files.exists(tempDir.resolve("users.ser")),
                "File users.ser phải được tạo sau khi đăng ký.");
    }

    @Test
    void register_InvalidEmail_MissingAt_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u1", "notanemail", "abc123", "BIDDER"),
                "Email không có @ phải ném IllegalArgumentException.");
    }

    @Test
    void register_InvalidEmail_NoDomainDot_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u2", "user@nodot", "abc123", "BIDDER"),
                "Email thiếu dấu chấm sau @ phải ném IllegalArgumentException.");
    }

    @Test
    void register_PasswordTooShort_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u3", "u3@mail.com", "12345", "BIDDER"),
                "Mật khẩu dưới 6 ký tự phải ném IllegalArgumentException.");
    }

    @Test
    void register_DuplicateUsername_ThrowsException() {
        authService.register("dup", "dup1@mail.com", "abc123", "BIDDER");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("dup", "dup2@mail.com", "abc123", "BIDDER"),
                "Username trùng phải ném IllegalArgumentException.");
    }

    @Test
    void register_DuplicateEmail_ThrowsException() {
        authService.register("user_a", "dupe@mail.com", "abc123", "BIDDER");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_b", "dupe@mail.com", "abc123", "BIDDER"),
                "Email trùng phải ném IllegalArgumentException.");
    }

    @Test
    void register_InvalidRole_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u4", "u4@mail.com", "abc123", "ADMIN"),
                "Vai trò không hợp lệ (ADMIN) phải ném IllegalArgumentException.");
    }

    @Test
    void register_EmptyUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "u5@mail.com", "abc123", "BIDDER"));
    }

    @Test
    void register_NullPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u6", "u6@mail.com", null, "BIDDER"));
    }

    // =========================================================================
    // login()
    // =========================================================================

    @Test
    void login_CorrectCredentials_ReturnsUser() {
        authService.register("loginUser", "login@mail.com", "mypass1", "BIDDER");

        Optional<User> result = authService.login("login@mail.com", "mypass1");

        assertTrue(result.isPresent(), "Đăng nhập đúng credentials phải trả về user.");
        assertEquals("loginUser", result.get().getUsername());
    }

    @Test
    void login_WrongPassword_ReturnsEmpty() {
        authService.register("loginUser2", "login2@mail.com", "correctPass", "BIDDER");

        Optional<User> result = authService.login("login2@mail.com", "wrongPass");

        assertTrue(result.isEmpty(), "Sai mật khẩu phải trả về Optional.empty().");
    }

    @Test
    void login_UnknownEmail_ReturnsEmpty() {
        Optional<User> result = authService.login("ghost@mail.com", "abc123");

        assertTrue(result.isEmpty(), "Email không tồn tại phải trả về Optional.empty().");
    }

    @Test
    void login_NullEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "abc123"),
                "Email null phải ném IllegalArgumentException.");
    }

    @Test
    void login_BlankEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("   ", "abc123"),
                "Email rỗng phải ném IllegalArgumentException.");
    }

    @Test
    void login_NullPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", null),
                "Password null phải ném IllegalArgumentException.");
    }

    @Test
    void login_BlankPassword_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", ""),
                "Password rỗng phải ném IllegalArgumentException.");
    }

    @Test
    void login_LegacyPlaintextPassword_UpgradedToHash() {
        authService.register("legacy", "legacy@mail.com", "legacyPass", "BIDDER");

        // Ghi đè password về plaintext để giả lập dữ liệu cũ
        User user = database.users().findByEmail("legacy@mail.com").orElseThrow();
        user.setPassword("legacyPass");
        database.users().save(user);
        database.flushAll();

        Optional<User> result = authService.login("legacy@mail.com", "legacyPass");
        assertTrue(result.isPresent(), "Login với plaintext cũ phải thành công.");

        // Sau login, password phải được nâng cấp sang hash
        database.reloadAll();
        User reloaded = database.users().findByEmail("legacy@mail.com").orElseThrow();
        assertEquals(SecurityUtils.hashPassword("legacyPass"), reloaded.getPassword(),
                "Password phải được nâng cấp sang hash sau login.");
    }

    // =========================================================================
    // isUsernameTaken() / isEmailTaken()
    // =========================================================================

    @Test
    void isUsernameTaken_ExistingUsername_ReturnsTrue() {
        authService.register("takenUser", "taken@mail.com", "abc123", "BIDDER");

        assertTrue(authService.isUsernameTaken("takenUser"));
    }

    @Test
    void isUsernameTaken_NewUsername_ReturnsFalse() {
        assertFalse(authService.isUsernameTaken("brandnewuser"));
    }

    @Test
    void isUsernameTaken_NullUsername_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isUsernameTaken(null));
    }

    @Test
    void isEmailTaken_ExistingEmail_ReturnsTrue() {
        authService.register("emailUser", "taken_email@mail.com", "abc123", "SELLER");

        assertTrue(authService.isEmailTaken("taken_email@mail.com"));
    }

    @Test
    void isEmailTaken_NewEmail_ReturnsFalse() {
        assertFalse(authService.isEmailTaken("new_email@mail.com"));
    }

    @Test
    void isEmailTaken_BlankEmail_ThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isEmailTaken("  "));
    }
}