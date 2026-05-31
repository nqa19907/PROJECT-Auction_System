import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import auction_system.common.models.users.Participant;
import auction_system.common.models.users.User;
import auction_system.common.utils.SecurityUtils;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.auth.request.LoginRequest;
import auction_system.server.services.auth.request.RegisterRequest;

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
        User user = authService.register(registerRequest(
                "participant01",
                "participant01@mail.com",
                "secret1",
                "PARTICIPANT"));

        assertNotNull(user);
        assertEquals("participant01", user.getUsername());
        assertEquals("PARTICIPANT", user.getRoleName());
    }

    @Test
    void registerValidAdminReturnsUserWithCorrectRole() {
        User user = authService.register(
                registerRequest("admin01", "admin01@mail.com", "secret1", "ADMIN"));

        assertNotNull(user);
        assertEquals("ADMIN", user.getRoleName());
    }

    @Test
    void registerPasswordIsHashedNotPlaintext() {
        User user = authService.register(registerRequest(
                "hashtest",
                "hashtest@mail.com",
                "plainpass",
                "PARTICIPANT"));

        assertNotEquals("plainpass", user.getPassword());
    }

    @Test
    void registerPasswordHashMatchesSha256() {
        String rawPassword = "plainpass";
        User user = authService.register(registerRequest(
                "hashtest2",
                "hashtest2@mail.com",
                rawPassword,
                "PARTICIPANT"));

        assertEquals(SecurityUtils.hashPassword(rawPassword), user.getPassword());
    }

    @Test
    void registerUserIsPersistedCanBeReloadedFromDatabase() {
        authService.register(
                registerRequest("persist01", "persist01@mail.com", "abc123", "PARTICIPANT"));

        database.reloadAll();

        Optional<User> found = database.users().findByEmail("persist01@mail.com");
        assertTrue(found.isPresent());
        assertEquals("persist01", found.get().getUsername());
    }

    @Test
    void registerSerFileExistsAfterRegistration() {
        authService.register(
                registerRequest("filecheck", "filecheck@mail.com", "abc123", "PARTICIPANT"));

        assertTrue(Files.exists(tempDir.resolve("users.ser")));
    }

    @Test
    void registerInvalidEmailMissingAtThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("u1", "notanemail", "abc123", "PARTICIPANT")));
    }

    @Test
    void registerInvalidEmailNoDomainDotThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("u2", "user@nodot", "abc123", "PARTICIPANT")));
    }

    @Test
    void registerPasswordTooShortThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("u3", "u3@mail.com", "12345", "PARTICIPANT")));
    }

    @Test
    void registerDuplicateUsernameThrowsException() {
        authService.register(registerRequest("dup", "dup1@mail.com", "abc123", "PARTICIPANT"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("dup", "dup2@mail.com", "abc123", "PARTICIPANT")));
    }

    @Test
    void registerDuplicateEmailThrowsException() {
        authService.register(
                registerRequest("user_a", "dupe@mail.com", "abc123", "PARTICIPANT"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("user_b", "dupe@mail.com", "abc123", "PARTICIPANT")));
    }

    @Test
    void registerInvalidRoleThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("u4", "u4@mail.com", "abc123", "UNKNOWN")));
    }

    @Test
    void registerEmptyUsernameThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("", "u5@mail.com", "abc123", "PARTICIPANT")));
    }

    @Test
    void registerNullPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(
                        registerRequest("u6", "u6@mail.com", null, "PARTICIPANT")));
    }

    @Test
    void loginCorrectCredentialsReturnsUser() {
        authService.register(
                registerRequest("loginUser", "login@mail.com", "mypass1", "PARTICIPANT"));

        Optional<User> result = authService.login(loginRequest("login@mail.com", "mypass1"));

        assertTrue(result.isPresent());
        assertEquals("loginUser", result.get().getUsername());
    }

    @Test
    void loginWrongPasswordReturnsEmpty() {
        authService.register(
                registerRequest("loginUser2", "login2@mail.com", "correctPass", "PARTICIPANT"));

        Optional<User> result = authService.login(loginRequest("login2@mail.com", "wrongPass"));

        assertTrue(result.isEmpty());
    }

    @Test
    void loginUnknownEmailReturnsEmpty() {
        Optional<User> result = authService.login(loginRequest("ghost@mail.com", "abc123"));

        assertTrue(result.isEmpty());
    }

    @Test
    void loginNullEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest(null, "abc123")));
    }

    @Test
    void loginBlankEmailThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest("   ", "abc123")));
    }

    @Test
    void loginNullPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest("a@b.com", null)));
    }

    @Test
    void loginBlankPasswordThrowsException() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.login(loginRequest("a@b.com", "")));
    }

    @Test
    void loginLegacyPlaintextPasswordUpgradedToHash() {
        authService.register(
                registerRequest("legacy", "legacy@mail.com", "legacyPass", "PARTICIPANT"));

        User user = database.users().findByEmail("legacy@mail.com").orElseThrow();
        user.setPassword("legacyPass");
        database.users().save(user);
        database.flushAll();

        Optional<User> result = authService.login(loginRequest("legacy@mail.com", "legacyPass"));
        assertTrue(result.isPresent());

        database.reloadAll();
        User reloaded = database.users().findByEmail("legacy@mail.com").orElseThrow();
        assertEquals(SecurityUtils.hashPassword("legacyPass"), reloaded.getPassword());
    }

    @Test
    void isUsernameTakenExistingUsernameReturnsTrue() {
        authService.register(
                registerRequest("takenUser", "taken@mail.com", "abc123", "PARTICIPANT"));

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
    void isEmailTakenExistingEmailReturnsTrue() {
        authService.register(
                registerRequest("emailUser", "taken_email@mail.com", "abc123", "PARTICIPANT"));

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

    // =========================================================================
    // deposit()
    // =========================================================================

    @Test
    void depositPositiveAmountIncreasesBalanceCorrectly() {
        authService.register(
                registerRequest("depositor", "dep@mail.com", "abc123", "PARTICIPANT"));
        User user = database.users().findByEmail("dep@mail.com").orElseThrow();

        double newBalance = authService.deposit(user, 500.0);

        assertEquals(500.0, newBalance, 0.001,
                "So du phai tang dung 500 sau khi nap.");
    }

    @Test
    void depositAccumulatesAcrossMultipleCalls() {
        authService.register(
                registerRequest("depositor2", "dep2@mail.com", "abc123", "PARTICIPANT"));
        User user = database.users().findByEmail("dep2@mail.com").orElseThrow();

        authService.deposit(user, 200.0);
        double newBalance = authService.deposit(user, 300.0);

        assertEquals(500.0, newBalance, 0.001,
                "So du phai la 500 sau hai lan nap 200 va 300.");
    }

    @Test
    void depositPersistedToDatabaseAfterDeposit() {
        authService.register(
                registerRequest("depositor3", "dep3@mail.com", "abc123", "PARTICIPANT"));
        User user = database.users().findByEmail("dep3@mail.com").orElseThrow();

        authService.deposit(user, 1000.0);

        database.reloadAll();
        User reloaded = database.users().findByEmail("dep3@mail.com").orElseThrow();
        assertEquals(1000.0,
                ((Participant) reloaded).getBalance(),
                0.001,
                "So du phai duoc luu xuong database sau khi nap.");
    }

    @Test
    void depositZeroAmountThrowsIllegalArgumentException() {
        authService.register(
                registerRequest("depositor4", "dep4@mail.com", "abc123", "PARTICIPANT"));
        User user = database.users().findByEmail("dep4@mail.com").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> authService.deposit(user, 0.0),
                "So tien nap bang 0 phai bi tu choi.");
    }

    @Test
    void depositNegativeAmountThrowsIllegalArgumentException() {
        authService.register(
                registerRequest("depositor5", "dep5@mail.com", "abc123", "PARTICIPANT"));
        User user = database.users().findByEmail("dep5@mail.com").orElseThrow();

        assertThrows(IllegalArgumentException.class,
                () -> authService.deposit(user, -100.0),
                "So tien nap am phai bi tu choi.");
    }

    @Test
    void depositAdminUserThrowsIllegalArgumentException() {
        User admin = authService.register(
                registerRequest("adminUser", "admin_dep@mail.com", "abc123", "ADMIN"));

        assertThrows(IllegalArgumentException.class,
                () -> authService.deposit(admin, 500.0),
                "Admin khong co vi, phai bi tu choi khi nap tien.");
    }
    private RegisterRequest registerRequest(
            final String username,
            final String email,
            final String password,
            final String roleName) {
        return new RegisterRequest(username, email, password, roleName);
    }

    private LoginRequest loginRequest(final String email, final String password) {
        return new LoginRequest(email, password);
    }
}
