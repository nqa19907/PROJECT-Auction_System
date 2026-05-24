import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 */
class AuthServiceDatabaseTest {

    @TempDir
    private Path tempDir;

    private SerializedDatabase database;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        database = new SerializedDatabase(tempDir);
        authService = new AuthService(database);
    }

    @Test
    void registerValidParticipantReturnsUserWithCorrectRole() {
        User user = authService.register(
                "participant01",
                "participant01@mail.com",
                "secret1",
                "PARTICIPANT");

        assertNotNull(user);
        assertEquals("participant01", user.getUsername());
        assertEquals("PARTICIPANT", user.getRoleName());
    }

    @Test
    void registerValidAdminReturnsUserWithCorrectRole() {
        User user = authService.register("admin01", "admin01@mail.com", "secret1", "ADMIN");

        assertNotNull(user);
        assertEquals("ADMIN", user.getRoleName());
    }

    @Test
    void registerPasswordIsHashedNotPlaintext() {
        User user = authService.register(
                "hashtest",
                "hashtest@mail.com",
                "plainpass",
                "PARTICIPANT");

        assertNotEquals("plainpass", user.getPassword());
    }

    @Test
    void registerPasswordHashMatchesSha256() {
        String rawPassword = "plainpass";
        User user = authService.register(
                "hashtest2",
                "hashtest2@mail.com",
                rawPassword,
                "PARTICIPANT");

        assertEquals(SecurityUtils.hashPassword(rawPassword), user.getPassword());
    }

    @Test
    void registerUserIsPersistedCanBeReloadedFromDatabase() {
        authService.register("persist01", "persist01@mail.com", "abc123", "PARTICIPANT");

        database.reloadAll();

        Optional<User> found = database.users().findByEmail("persist01@mail.com");
        assertTrue(found.isPresent());
        assertEquals("persist01", found.get().getUsername());
    }

    @Test
    void registerSerFileExistsAfterRegistration() {
        authService.register("filecheck", "filecheck@mail.com", "abc123", "PARTICIPANT");

        assertTrue(Files.exists(tempDir.resolve("users.ser")));
    }

    @Test
    void registerInvalidEmailMissingAtThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u1", "notanemail", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerInvalidEmailNoDomainDotThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u2", "user@nodot", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerPasswordTooShortThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u3", "u3@mail.com", "12345", "PARTICIPANT"));
    }

    @Test
    void registerPasswordExactlyMinLengthDoesNotThrow() {
        assertDoesNotThrow(
                () -> authService.register("u3b", "u3b@mail.com", "123456", "PARTICIPANT"));
    }

    @Test
    void registerDuplicateUsernameThrowsException() {
        authService.register("dup", "dup1@mail.com", "abc123", "PARTICIPANT");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("dup", "dup2@mail.com", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerDuplicateEmailThrowsException() {
        authService.register("user_a", "dupe@mail.com", "abc123", "PARTICIPANT");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_b", "dupe@mail.com", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerDuplicateEmailCaseInsensitiveThrowsException() {
        authService.register("user_a", "dup@mail.com", "abc123", "PARTICIPANT");

        assertThrows(IllegalArgumentException.class,
                () -> authService.register("user_b", "DUP@MAIL.COM", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerInvalidRoleThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u4", "u4@mail.com", "abc123", "SELLER"));
    }

    @Test
    void registerEmptyUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("", "u5@mail.com", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerBlankUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("   ", "u5b@mail.com", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerNullUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(null, "u7@mail.com", "abc123", "PARTICIPANT"));
    }

    @Test
    void registerNullEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u8", null, "abc123", "PARTICIPANT"));
    }

    @Test
    void registerNullPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u6", "u6@mail.com", null, "PARTICIPANT"));
    }

    @Test
    void registerNullRoleThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register("u9", "u9@mail.com", "abc123", null));
    }

    @Test
    void loginCorrectCredentialsReturnsUser() {
        authService.register("loginUser", "login@mail.com", "mypass1", "PARTICIPANT");

        Optional<User> result = authService.login("login@mail.com", "mypass1");

        assertTrue(result.isPresent());
        assertEquals("loginUser", result.get().getUsername());
    }

    @Test
    void loginWrongPasswordReturnsEmpty() {
        authService.register("loginUser2", "login2@mail.com", "correctPass", "PARTICIPANT");

        Optional<User> result = authService.login("login2@mail.com", "wrongPass");

        assertTrue(result.isEmpty());
    }

    @Test
    void loginUnknownEmailReturnsEmpty() {
        Optional<User> result = authService.login("ghost@mail.com", "abc123");

        assertTrue(result.isEmpty());
    }

    @Test
    void loginEmailCaseInsensitiveReturnsUser() {
        authService.register("caseUser", "case@mail.com", "mypass1", "PARTICIPANT");

        Optional<User> result = authService.login("CASE@MAIL.COM", "mypass1");

        assertTrue(result.isPresent());
    }

    @Test
    void loginNullEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(null, "abc123"));
    }

    @Test
    void loginBlankEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("   ", "abc123"));
    }

    @Test
    void loginNullPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", null));
    }

    @Test
    void loginBlankPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login("a@b.com", ""));
    }

    @Test
    void loginLegacyPlaintextPasswordUpgradedToHash() {
        authService.register("legacy", "legacy@mail.com", "legacyPass", "PARTICIPANT");

        User user = database.users().findByEmail("legacy@mail.com").orElseThrow();
        user.setPassword("legacyPass");
        database.users().save(user);
        database.flushAll();

        Optional<User> result = authService.login("legacy@mail.com", "legacyPass");
        assertTrue(result.isPresent());

        database.reloadAll();
        User reloaded = database.users().findByEmail("legacy@mail.com").orElseThrow();
        assertEquals(SecurityUtils.hashPassword("legacyPass"), reloaded.getPassword());
    }

    @Test
    void isUsernameTakenExistingUsernameReturnsTrue() {
        authService.register("takenUser", "taken@mail.com", "abc123", "PARTICIPANT");

        assertTrue(authService.isUsernameTaken("takenUser"));
    }

    @Test
    void isUsernameTakenNewUsernameReturnsFalse() {
        assertFalse(authService.isUsernameTaken("brandnewuser"));
    }

    @Test
    void isUsernameTakenNullUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isUsernameTaken(null));
    }

    @Test
    void isUsernameTakenBlankUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isUsernameTaken("   "));
    }

    @Test
    void isEmailTakenExistingEmailReturnsTrue() {
        authService.register("emailUser", "taken_email@mail.com", "abc123", "PARTICIPANT");

        assertTrue(authService.isEmailTaken("taken_email@mail.com"));
    }

    @Test
    void isEmailTakenNewEmailReturnsFalse() {
        assertFalse(authService.isEmailTaken("new_email@mail.com"));
    }

    @Test
    void isEmailTakenBlankEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isEmailTaken("  "));
    }

    @Test
    void isEmailTakenNullEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.isEmailTaken(null));
    }
}
