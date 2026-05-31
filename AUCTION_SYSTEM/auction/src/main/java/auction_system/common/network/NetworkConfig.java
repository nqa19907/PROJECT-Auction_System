package auction_system.common.network;

/**
 * Lớp cấu hình chứa các thông số kết nối mạng cho hệ thống đấu giá.
 */
public class NetworkConfig {
    public static final String DEFAULT_SERVER_HOST = "127.0.0.1";
    public static final int DEFAULT_SERVER_PORT = 8080;

    public static final String SERVER_HOST = readStringConfig(
            "auction.server.host",
            "AUCTION_SERVER_HOST",
            DEFAULT_SERVER_HOST);
    public static final int SERVER_PORT = readIntConfig(
            "auction.server.port",
            "AUCTION_SERVER_PORT",
            DEFAULT_SERVER_PORT);
    
    // Private constructor để không ai tạo đối tượng từ class cấu hình
    private NetworkConfig() {
    }

    private static String readStringConfig(
            final String propertyName,
            final String environmentName,
            final String defaultValue) {
        final String propertyValue = System.getProperty(propertyName);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        final String environmentValue = System.getenv(environmentName);
        if (environmentValue != null && !environmentValue.isBlank()) {
            return environmentValue.trim();
        }

        return defaultValue;
    }

    private static int readIntConfig(
            final String propertyName,
            final String environmentName,
            final int defaultValue) {
        final String rawValue = readStringConfig(propertyName, environmentName, "");
        if (rawValue.isBlank()) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(rawValue);
        } catch (NumberFormatException exception) {
            return defaultValue;
        }
    }
}
