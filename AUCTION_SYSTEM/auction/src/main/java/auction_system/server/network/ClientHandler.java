package auction_system.server.network;

import auction_system.client.services.AuthService;
import auction_system.common.models.auctions.Auction;
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
import auction_system.server.services.AuctionBidService;
import auction_system.server.session.ClientSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Xử lý giao tiếp với một client duy nhất.
 *
 * <p>Chạy trên một thread riêng (từ thread pool của {@link SocketServer})
 * và đồng thời đóng vai trò {@link AuctionObserver}: khi có sự kiện đấu giá,
 * {@link Auction#notifyObservers(String)} tự động gọi {@link #update(String)}
 * để đẩy thông báo xuống client này qua socket — không cần polling.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AuthService authService;
    private final AuctionBidService auctionBidService;
    private final Map<String, Command> commandMap;
    private final ClientSession session;
    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Khởi tạo handler cho một kết nối client.
     *
     * @param socket            Socket kết nối từ client.
     * @param auctionManager    Quản lý thông tin các phiên đấu giá.
     * @param authService       Dịch vụ xác thực tài khoản.
     * @param auctionBidService Dịch vụ đặt giá.
     */
    public ClientHandler(
            final Socket socket, 
            final AuctionManager auctionManager,
            final AuthService authService,
            final AuctionBidService auctionBidService) { 
        this.socket = Objects.requireNonNull(socket, "socket");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.session = new ClientSession(this, auctionManager);
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
        
        /*
         * Bản đồ ánh xạ từ tên lệnh theo giao thức sang đối tượng xử lý (Command) tương ứng.
         */
        this.commandMap = Map.ofEntries(
                Map.entry(Protocol.Command.LOGIN.name(),
                        new LoginCommand(auctionManager)),
                Map.entry(Protocol.Command.REGISTER.name(),
                        new RegisterCommand(auctionManager)),
                Map.entry(Protocol.Command.LIST_AUCTIONS.name(),
                        new ListAuctionsCommand(auctionManager)),
                Map.entry(Protocol.Command.GET_AUCTION.name(),
                        new GetAuctionCommand(auctionManager)),
                Map.entry(Protocol.Command.JOIN_AUCTION.name(),
                        new JoinAuctionCommand(auctionManager)),
                Map.entry(Protocol.Command.LEAVE_AUCTION.name(),
                        new LeaveAuctionCommand(auctionManager)),
                Map.entry(Protocol.Command.PLACE_BID.name(),
                        new PlaceBidCommand(auctionBidService)),
                Map.entry(Protocol.Command.LOGOUT.name(),
                        new LogoutCommand(auctionManager))
        );
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
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)
            );
            outputWriter = new PrintWriter(
                    socket.getOutputStream(), true, StandardCharsets.UTF_8
            );

            String line;
            while ((line = inputReader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    handleCommand(line);
                }
            }

        } catch (IOException exception) {
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
    private void handleCommand(final String rawCommand) {
        final String[] parts = rawCommand.split(Protocol.SEPARATOR_REGEX, -1);
        final String commandName = parts[0].toUpperCase();

        final Command command = commandMap.get(commandName);
        if (command != null) {
            final String response = command.execute(parts, session);
            if (response != null) {
                // Xử lý các phản hồi nhiều dòng (ví dụ: từ LIST_AUCTIONS)
                // Command sẽ trả về một chuỗi duy nhất với các dòng được phân tách bằng '\n'
                for (final String line : response.split("\n")) {
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
    public void update(final String message) {
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
    private synchronized void send(final String message) {
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

        final User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            auctionManager.userLoggedOut(currentUser);
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
        } catch (IOException exception) {
            LOGGER.warning("Lỗi khi đóng tài nguyên client: " + exception.getMessage());
        }
    }
}