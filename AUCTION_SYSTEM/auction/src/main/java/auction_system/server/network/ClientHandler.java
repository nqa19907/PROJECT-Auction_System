package auction_system.server.network;

import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.network.command.GetAuctionCommand;
import auction_system.server.network.command.JoinAuctionCommand;
import auction_system.server.network.command.LeaveAuctionCommand;
import auction_system.server.network.command.ListAuctionsCommand;
import auction_system.server.network.command.LoginCommand;
import auction_system.server.network.command.LogoutCommand;
import auction_system.server.network.command.PlaceBidCommand;
import auction_system.server.network.command.RegisterCommand;
import auction_system.server.session.ClientSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý giao tiếp với một client duy nhất.
 *
 * <p>Chạy trên một thread riêng (từ thread pool của {@link SocketServer})
 * và đồng thời đóng vai trò {@link AuctionObserver}: khi có sự kiện đấu giá,
 * {@link Auction#notifyObservers(String)} tự động gọi {@link #update(String)}
 * để đẩy thông báo xuống client này qua socket — không cần polling.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Phiên làm việc lưu trữ trạng thái của client hiện tại
     * (người dùng, phiên đấu giá đang xem).
     */
    private final ClientSession session;

    /**
     * Bản đồ ánh xạ từ tên lệnh theo giao thức sang đối tượng xử lý (Command) tương ứng.
     */
    private static final Map<String, Command> commandMap = Map.ofEntries(
        Map.entry(Protocol.Command.LOGIN.name(), new LoginCommand()),
        Map.entry(Protocol.Command.REGISTER.name(), new RegisterCommand()),
        Map.entry(Protocol.Command.LIST_AUCTIONS.name(), new ListAuctionsCommand()),
        Map.entry(Protocol.Command.GET_AUCTION.name(), new GetAuctionCommand()),
        Map.entry(Protocol.Command.JOIN_AUCTION.name(), new JoinAuctionCommand()),
        Map.entry(Protocol.Command.LEAVE_AUCTION.name(), new LeaveAuctionCommand()),
        Map.entry(Protocol.Command.PLACE_BID.name(), new PlaceBidCommand()),
        Map.entry(Protocol.Command.LOGOUT.name(), new LogoutCommand())
    );

    /**
     * Khởi tạo handler cho một kết nối client.
     *
     * @param socket Socket kết nối từ client.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.session = new ClientSession(this);
    }

    // =========================================================================
    // Runnable — vòng lặp đọc lệnh từ client
    // =========================================================================
    
    /**
     * Vòng lặp chính của luồng (thread): liên tục lắng nghe và đọc dữ liệu 
     * văn bản gửi lên từ client, sau đó chuyển giao cho trình xử lý lệnh.
     */
    @Override
    public void run() {
        try {
            inputReader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), 
                            StandardCharsets.UTF_8)
            );
            outputWriter = new PrintWriter(
                            socket.getOutputStream(), true,
                            StandardCharsets.UTF_8
            );

            String line;
            while ((line = inputReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    handleCommand(line);
                }
            }

        } catch (IOException e) {
            LOGGER.info("Client ngắt kết nối đột ngột: " + socket.getInetAddress());
        } finally {
            cleanup();
        }
    }

    // =========================================================================
    // Phân tích và điều phối lệnh
    // =========================================================================

    /**
     * Phân tích lệnh thô và gọi handler tương ứng.
     * Định dạng: {@code COMMAND|param1|param2|...}
     *
     * @param rawCommand Dòng lệnh thô nhận từ client.
     */
    private void handleCommand(String rawCommand) {
        String[] parts = rawCommand.split(Protocol.SEPARATOR_REGEX, -1);
        String commandName = parts[0].toUpperCase();

        Command command = commandMap.get(commandName);
        if (command != null) {
            String response = command.execute(parts, session);
            if (response != null) {
                // Xử lý các phản hồi nhiều dòng (ví dụ: từ LIST_AUCTIONS)
                // Command sẽ trả về một chuỗi duy nhất với các dòng được phân tách bằng '\n'
                for (String line : response.split("\n")) {
                    send(line);
                }
            }
        } else {
            send(Protocol.Response.ERROR.name() + Protocol.SEPARATOR 
                    + "Lệnh không hợp lệ: " + commandName);
        }
    }

    // =========================================================================
    // AuctionObserver
    // =========================================================================

    /**
     * Nhận thông báo realtime từ phiên đấu giá và đẩy xuống client.
     *
     * @param message Thông điệp theo định dạng giao thức.
     */
    @Override
    public void update(String message) {
        send(message);
    }

    // =========================================================================
    // Tiện ích
    // =========================================================================

    /**
     * Gửi một dòng văn bản tới client qua socket.
     * Synchronized để tránh 2 thread ghi đồng thời.
     *
     * @param message Nội dung cần gửi.
     */
    private synchronized void send(String message) {
        if (outputWriter != null && !socket.isClosed()) {
            outputWriter.println(message);
        }
    }

    /**
     * Dọn dẹp tài nguyên khi client ngắt kết nối.
     * Luôn được gọi trong finally của {@link #run()}.
     */
    private void cleanup() {
        // Đảm bảo người dùng được đăng xuất và hủy theo dõi tất cả các phiên đấu giá
        session.leaveAllAuctions();

        User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            AuctionManager.getInstance().userLoggedOut(currentUser);
            currentUser.setOnline(false);
            LOGGER.info("Cleanup session: " + currentUser.getUsername());
            session.setCurrentUser(null);
        }

        try {
            if (inputReader != null) {
                inputReader.close();
            }
            if (outputWriter != null) {
                outputWriter.close();
            }
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            LOGGER.warn("Lỗi khi đóng tài nguyên client: " + e.getMessage());
        }
    }
}
