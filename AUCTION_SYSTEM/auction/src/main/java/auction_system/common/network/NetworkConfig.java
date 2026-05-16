package auction_system.common.network;

/**
 * Lớp cấu hình chứa các thông số kết nối mạng cho hệ thống đấu giá.
 */
public class NetworkConfig {
    public static final String SERVER_HOST = "127.0.0.1"; // 127.0.0.1 đồng nghĩa với localhost
    public static final int SERVER_PORT = 8080;
    
    // Private constructor để không ai tạo đối tượng từ class cấu hình
    private NetworkConfig() {
    }
}