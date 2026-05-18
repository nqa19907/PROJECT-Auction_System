package auction_system.client.network;

import auction_system.common.network.Protocol;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final Map<String, CopyOnWriteArrayList<Consumer<String>>>
        messageHandlers = new ConcurrentHashMap<>(); 
    
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
     * Đăng ký một hàm xử lý cho một loại lệnh cụ thể từ Server.
     *
     * @param command Tên lệnh (VD: "LOGIN_OK", "UPDATE_PRICE").
     * @param handler Hàm xử lý thông điệp.
     */
    public void registerHandler(String command, Consumer<String> handler) {
        // Lệnh computeIfAbsent sẽ kiểm tra: Nếu command này chưa
        // có danh sách thì tự tạo mới (CopyOnWriteArrayList), còn có
        // rồi thì dùng cái cũ.
        // Sau đó, nó thêm handler của bạn vào danh sách đó.
        messageHandlers.computeIfAbsent(command, k -> new CopyOnWriteArrayList<>()).add(handler);
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
     * Gửi một lệnh (Command) lên Server.
     *
     * @param command Chuỗi lệnh cần gửi.
     * @return true nếu gửi thành công, false nếu chưa kết nối hoặc có lỗi.
     */
    public boolean sendCommand(String command) {
        if (out != null && !socket.isClosed()) {
            out.println(command);
            LOGGER.info("Gửi tới Server: " + command);
            return true;
        } else {
            LOGGER.warning("Không thể gửi lệnh, chưa kết nối: " + command);
            return false;
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

                // --- BẮT ĐẦU BỘ ĐỊNH TUYẾN (ROUTER) ---

                // Bước 1: Tách chuỗi lấy tên lệnh (Chìa khóa)
                String[] parts = msg.split(Protocol.SEPARATOR_REGEX);
                String command = parts[0];

                // Bước 2: Tra cứu danh bạ
                CopyOnWriteArrayList<Consumer<String>> handlers = messageHandlers.get(command);

                // Bước 3: Phân tán tin nhắn
                if (handlers != null && !handlers.isEmpty()) {
                    // Nếu có người hóng lệnh này, duyệt qua từng người và gửi tin nhắn
                    for (Consumer<String> handler : handlers) {
                        Platform.runLater(() -> handler.accept(msg));
                    }
                } else {
                    LOGGER.warning("Chưa có màn hình nào đăng ký xử lý lệnh: " + command);
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
            // Phải đóng Socket TRƯỚC để bẻ gãy blocking I/O (readLine) đang kẹt ở luồng lắng nghe
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
            if (in != null) {
                in.close();
            }
            if (out != null) {
                out.close();
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
