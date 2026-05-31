package auction_system.client.network;

import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
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
import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quản lý kết nối mạng từ phía Client gửi tới Server.
 * Áp dụng Singleton để toàn bộ giao diện dùng chung một đường ống.
 */
public class NetworkClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkClient.class);
    private static volatile NetworkClient instance;

    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private Thread listeningThread;
    private volatile boolean isRunning = false;
    private String lastHost;
    private int lastPort;
    
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
     * Gỡ một hàm xử lý khỏi loại phản hồi đã đăng ký.
     *
     * @param command tên phản hồi từ server
     * @param handler hàm xử lý cần gỡ
     */
    public void unregisterHandler(String command, Consumer<String> handler) {
        final CopyOnWriteArrayList<Consumer<String>> handlers = messageHandlers.get(command);
        if (handlers == null) {
            return;
        }

        handlers.remove(handler);
        if (handlers.isEmpty()) {
            messageHandlers.remove(command, handlers);
        }
    }

    /**
     * Mở kết nối tới Server.
     *
     * @param host Địa chỉ máy chủ.
     * @param port Cổng kết nối.
     * @throws IOException Nếu có lỗi xảy ra khi kết nối.
     */
    public synchronized void connect(String host, int port) throws IOException {
        if (isConnected()) {
            return; // Đã kết nối
        }

        lastHost = host;
        lastPort = port;
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
     * Kiem tra socket hien tai con dung duoc hay khong.
     *
     * @return true neu client dang co ket noi mo toi server
     */
    public boolean isConnected() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && out != null
                && in != null;
    }

    /**
     * Dam bao client co ket noi truoc khi gui lenh nghiep vu.
     *
     * <p>Neu socket bi dong sau logout hoac do loi mang tam thoi, client se thu ket noi
     * lai bang host/port gan nhat da dung khi ung dung khoi dong.
     *
     * @return true neu da co hoac da khoi phuc duoc ket noi
     */
    public synchronized boolean ensureConnected() {
        if (isConnected()) {
            return true;
        }

        if (lastHost == null || lastHost.isBlank()) {
            return false;
        }

        try {
            connect(lastHost, lastPort);
            return true;
        } catch (IOException exception) {
            LOGGER.warn("Khong the ket noi lai toi server: {}", exception.getMessage());
            return false;
        }
    }

    /**
     * Gửi một message JSON lên server.
     *
     * @param message message cần gửi
     * @return true nếu gửi thành công, false nếu chưa kết nối hoặc serialize lỗi
     */
    public boolean sendMessage(final JsonMessage message) {
        final String rawMessage;
        try {
            rawMessage = JsonProtocol.stringify(message);
        } catch (IOException exception) {
            LOGGER.warn("Không thể serialize JSON message: {}", exception.getMessage());
            return false;
        }

        if (isConnected()) {
            out.println(rawMessage);
            LOGGER.info("Gửi tới Server: " + rawMessage);
            return true;
        } else {
            LOGGER.warn("Không thể gửi message, chưa kết nối: " + rawMessage);
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

                // Bước 1: Lấy tên phản hồi làm chìa khóa route handler.
                String command = extractRouteKey(msg);
                if (command == null || command.isBlank()) {
                    LOGGER.warn("Không xác định được loại phản hồi từ Server: " + msg);
                    continue;
                }

                // Bước 2: Tra cứu danh bạ
                CopyOnWriteArrayList<Consumer<String>> handlers = messageHandlers.get(command);

                // Bước 3: Phân tán tin nhắn
                if (handlers != null && !handlers.isEmpty()) {
                    // Nếu có người hóng lệnh này, duyệt qua từng người và gửi tin nhắn
                    for (Consumer<String> handler : handlers) {
                        Platform.runLater(() -> handler.accept(msg));
                    }
                } else {
                    LOGGER.warn("Chưa có màn hình nào đăng ký xử lý lệnh: " + command);
                }
            }
        } catch (IOException e) {
            if (isRunning) {
                LOGGER.warn("Mất kết nối tới server: " + e.getMessage());
            }
        } finally {
            cleanupConnection();
        }
    }

    /**
     * Lấy route key từ response JSON.
     *
     * @param message dòng phản hồi từ server
     * @return tên response dùng để tìm handler
     */
    private String extractRouteKey(final String message) {
        try {
            final JsonMessage jsonMessage = JsonProtocol.parse(message);
            return jsonMessage.type();
        } catch (IOException exception) {
            LOGGER.warn("Không parse được JSON response: {}", exception.getMessage());
            return null;
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
            LOGGER.error("Lỗi khi đóng kết nối: " + e.getMessage());
        } finally {
            socket = null;
            in = null;
            out = null;
            isRunning = false;
        }
    }
}
