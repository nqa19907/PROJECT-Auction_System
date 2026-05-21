
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import auction_system.common.models.users.User;
import auction_system.server.persistence.serialization.SerializedDatabase;
import auction_system.server.services.AuthService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Kiểm thử việc đăng ký tài khoản và ghi người dùng xuống database serialization.
 */
class AuthServiceDatabaseTest {
    @TempDir
    private Path tempDir;

    @Test
    void registerShouldWriteUserToSerializedDatabase() {
        SerializedDatabase database = new SerializedDatabase(tempDir);
        AuthService authService = new AuthService(database);

        authService.register(
                "testuser01",
                "testuser01@example.com",
                "123456",
                "BIDDER");

        database.reloadAll();

        Optional<User> foundUser = database.users().findByEmail("testuser01@example.com");

        assertTrue(foundUser.isPresent());
        assertEquals("testuser01", foundUser.get().getUsername());
        assertEquals("PARTICIPANT", foundUser.get().getRoleName());
        assertNotEquals("123456", foundUser.get().getPassword());
        assertTrue(Files.exists(tempDir.resolve("users.ser")));
    }
}