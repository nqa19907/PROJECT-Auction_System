package auction_system.client.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 * Quản lý kết nối mạng từ phía Client gửi tới Server.
 * Áp dụng Singleton để toàn bộ giao diện dùng chung một đường ống.
 */
public class NetworkClient {
    private static final Logger LOGGER = Logger.getLogger(NetworkClient.class.getName());
    private static volatile NetworkClient instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listeningThread;
    private volatile boolean isRunning = false;
    
    // Callback để đẩy dữ liệu cho Controller (Giao diện)
    private Consumer<String> messageHandler;
    
    private NetworkClient() {

    }

    /**
     * Lấy instance duy nhất của NetworkClient.
     *
     * @return Instance của NetworkClient.
     */
    public static NetworkClient getInstance() {
        // Kiểm tra lần 1: Tránh dùng khóa synchronized nếu đã được khởi tạo, tối ưu hiệu năng.
        if (instance == null) {
            // Sử dụng Class-level lock đảm bảo tại 1 thời điểm chỉ 1 luồng duy nhất lọt vào.
            synchronized (NetworkClient.class) {
                // Kiểm tra lần 2 (Double check): Ngăn việc nhiều luồng vượt qua lần kiểm thứ 1.
                if (instance == null) {
                    instance = new NetworkClient();
                }
            }
        }
        return instance;
    }

    /**
     * Mở kết nối tới Server.
     *
     * @param host Địa chỉ máy chủ.
     * @param port Cổng kết nối.
     * @throws IOException Nếu có lỗi xảy ra khi kết nối.
     */
    public void connect(String host, int port) throws IOException {
        if (socket != null && !socket.isClosed()) {
            return; // Đã kết nối
        }

        socket = new Socket(host, port);
        out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

        isRunning = true;

        // Khởi tạo luồng lắng nghe tin nhắn từ Server
        listeningThread = new Thread(this::listenToServer);
        // Đảm bảo luồng chết khi tắt app JavaFX
        listeningThread.setDaemon(true);
        listeningThread.start();

        LOGGER.info("Đã kết nối tới Server tại " + host + ": " + port);
    }

    /**
     * Controller của JavaFX đăng ký hàm xử lý qua phương thức này.
     *
     * @param messageHandler Hàm xử lý thông điệp từ server.
     */
    public void setMessageHandler(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    /**
     * Gửi một lệnh (Command) lên Server.
     *
     * @param command Chuỗi lệnh cần gửi.
     */
    public void sendCommand(String command) {
        if (out != null && !socket.isClosed()) {
            out.println(command);
            LOGGER.info("Gửi tới Server: " + command);
        } else {
            LOGGER.warning("Không thể gửi lệnh, chưa kết nối: " + command);
        }
    }

    /**
     * Lắng nghe và xử lý tin nhắn nhận được từ Server.
     */
    private void listenToServer() {
        try {
            String response;
            while (isRunning && (response = in.readLine()) != null) {
                final String msg = response;
                LOGGER.info("Nhận từ Server: " + msg);

                if (messageHandler != null) {
                    // Ép việc cập nhật giao diện chạy trên JavaFX Application Thread
                    Platform.runLater(() -> messageHandler.accept(msg));
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                LOGGER.warning("Mất kết nối tới server: " + e.getMessage());
            }
        } finally {
            cleanupConnection();
        }
    }

    /**
     * Đóng kết nối và dọn dẹp tài nguyên.
     * Phương thức này nên được gọi khi ứng dụng đóng.
     */
    public synchronized void disconnect() {
        if (!isRunning) {
            return;
        }
        isRunning = false;
        LOGGER.info("Bắt đầu ngắt kết nối khỏi Server...");

        if (listeningThread != null && listeningThread.isAlive()) {
            listeningThread.interrupt();
        }
        
        cleanupConnection();
    }

    /**
     * Dọn dẹp các tài nguyên mạng (socket, streams).
     */
    private void cleanupConnection() {
        try {
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            LOGGER.info("Đã ngắt kết nối và dọn dẹp tài nguyên.");
        } catch (IOException e) {
            LOGGER.severe("Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            socket = null;
            in = null;
            out = null;
            isRunning = false;
        }
    }
}
