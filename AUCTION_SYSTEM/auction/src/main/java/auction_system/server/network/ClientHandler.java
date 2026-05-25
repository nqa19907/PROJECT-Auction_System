package auction_system.server.network;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.AdminCancelAuctionCommand;
import auction_system.server.network.command.AdminDeleteAuctionCommand;
import auction_system.server.network.command.AdminDeleteUserCommand;
import auction_system.server.network.command.AdminListAuctionsCommand;
import auction_system.server.network.command.AdminListUsersCommand;
import auction_system.server.network.command.Command;
import auction_system.server.network.command.DepositCommand;
import auction_system.server.network.command.GetAuctionCommand;
import auction_system.server.network.command.GetBidHistoryCommand;
import auction_system.server.network.command.ListAuctionsCommand;
import auction_system.server.network.command.LoginCommand;
import auction_system.server.network.command.LogoutCommand;
import auction_system.server.network.command.PlaceBidCommand;
import auction_system.server.network.command.PublishItemCommand;
import auction_system.server.network.command.RegisterCommand;
import auction_system.server.network.command.UnwatchAuctionCommand;
import auction_system.server.network.command.WatchAuctionCommand;
import auction_system.server.services.AuctionBidService;
import auction_system.server.services.AuthService;
import auction_system.server.services.ParticipantItemService;
import auction_system.server.session.ClientSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Xử lý giao tiếp với một client duy nhất.
 *
 * <p>Chạy trên một thread riêng và đồng thời đóng vai trò {@link AuctionObserver}.
 * Khi phiên đấu giá có thay đổi, {@link Auction} sẽ gọi {@link #update(String)}
 * để đẩy thông báo realtime về client.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientHandler.class);

    private final Socket socket;
    private final AuctionManager auctionManager;
    private final AuthService authService;
    private final AuctionBidService auctionBidService;
    private final ParticipantItemService participantItemService;
    private final Map<String, Command> commandMap;
    private final ClientSession session;

    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Khởi tạo handler cho một kết nối client.
     *
     * @param socket socket kết nối từ client
     * @param auctionManager manager quản lý phiên đấu giá và trạng thái online
     * @param authService service xác thực tài khoản bằng database
     * @param auctionBidService service xử lý đặt giá
     * @param participantItemService service xử lý item người dùng
     */
    public ClientHandler(
            final Socket socket,
            final AuctionManager auctionManager,
            final AuthService authService,
            final AuctionBidService auctionBidService,
            final ParticipantItemService participantItemService) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
        this.participantItemService =
                Objects.requireNonNull(participantItemService, "participantItemService");
        this.session = new ClientSession(this, auctionManager);
        this.commandMap = createCommandMap();
    }

    /**
     * Tạo bảng ánh xạ command từ client sang command handler tương ứng.
     *
     * @return map chứa các command handler của kết nối hiện tại
     */
    private Map<String, Command> createCommandMap() {
        return Map.ofEntries(
                Map.entry(
                        Protocol.Command.LOGIN.name(),
                        new LoginCommand(authService, auctionManager)),
                Map.entry(
                        Protocol.Command.REGISTER.name(),
                        new RegisterCommand(authService)),
                Map.entry(
                        Protocol.Command.LIST_AUCTIONS.name(),
                        new ListAuctionsCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.GET_AUCTION.name(),
                        new GetAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.GET_BID_HISTORY.name(),
                        new GetBidHistoryCommand(auctionBidService)),
                Map.entry(
                        Protocol.Command.WATCH_AUCTION.name(),
                        new WatchAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.UNWATCH_AUCTION.name(),
                        new UnwatchAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.PLACE_BID.name(),
                        new PlaceBidCommand(auctionBidService)),
                Map.entry(
                        Protocol.Command.DEPOSIT.name(),
                        new DepositCommand(authService)),
                Map.entry(
                        Protocol.Command.LOGOUT.name(),
                        new LogoutCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.PUBLISH_ITEM.name(),
                        new PublishItemCommand(participantItemService, auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_LIST_USERS.name(),
                        new AdminListUsersCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_LIST_AUCTIONS.name(),
                        new AdminListAuctionsCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_CANCEL_AUCTION.name(),
                        new AdminCancelAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_DELETE_AUCTION.name(),
                        new AdminDeleteAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_DELETE_USER.name(),
                        new AdminDeleteUserCommand(auctionManager)));
    }

    /**
     * Lắng nghe dữ liệu từ client và chuyển tiếp đến command handler.
     */
    @Override
    public void run() {
        try {
            inputReader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            outputWriter = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);

            String line;
            while ((line = inputReader.readLine()) != null) {
                final String commandText = line.trim();

                if (!commandText.isEmpty()) {
                    handleCommand(commandText);
                }
            }
        } catch (IOException exception) {
            LOGGER.info("Client ngắt kết nối đột ngột: {}", socket.getInetAddress());
        } finally {
            cleanup();
        }
    }

    /**
     * Phân tích lệnh thô và gọi command handler tương ứng.
     *
     * @param rawCommand dòng lệnh thô nhận từ client
     */
    private void handleCommand(final String rawCommand) {
        final String[] parts = rawCommand.split(Protocol.SEPARATOR_REGEX, -1);
        final String commandName = parts[0].toUpperCase();
        final Command command = commandMap.get(commandName);

        if (command == null) {
            send(
                    Protocol.Response.ERROR.name()
                            + Protocol.SEPARATOR
                            + "Lệnh không hợp lệ: "
                            + commandName);
            return;
        }

        final String response = command.execute(parts, session);
        if (response != null) {
            sendResponseLines(response);
        }
    }

    /**
     * Gửi từng dòng phản hồi về client.
     *
     * @param response phản hồi có thể gồm một hoặc nhiều dòng
     */
    private void sendResponseLines(final String response) {
        final String[] responseLines = response.split("\n");

        for (final String responseLine : responseLines) {
            send(responseLine);
        }
    }

    /**
     * Nhận thông báo realtime từ phiên đấu giá và gửi về client.
     *
     * @param message thông điệp realtime theo định dạng protocol
     */
    @Override
    public void update(final String message) {
        send(message);
    }

    /**
     * Gửi realtime message trực tiếp tới client đang giữ socket này.
     *
     * <p>Method này dùng cho thông báo theo user, ví dụ cập nhật ví, không phụ
     * thuộc vào việc client đang theo dõi phiên đấu giá nào.
     *
     * @param message message cần gửi xuống client
     */
    public void sendDirect(final String message) {
        send(message);
    }

    /**
     * Gửi một dòng văn bản tới client qua socket.
     * Synchronized để tránh 2 thread ghi đồng thời.
     *
     * @param message nội dung cần gửi
     */
    private synchronized void send(final String message) {
        if (outputWriter != null && !socket.isClosed()) {
            outputWriter.println(message);
        }
    }

    /**
     * Dọn dẹp tài nguyên và xóa trạng thái online khi client ngắt kết nối.
     */
    private void cleanup() {
        session.unwatchAllAuctions();

        final User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            auctionManager.userLoggedOut(currentUser);

            // Gỡ socket handler khi client mất kết nối hoặc đóng ứng dụng.
            auctionManager.unregisterClientHandler(currentUser.getId(), this);

            currentUser.setOnline(false);
            LOGGER.info("Dọn dẹp phiên làm việc của: {}", currentUser.getUsername());
            session.setCurrentUser(null);
        }

        closeResources();
    }

    /**
     * Đóng các tài nguyên mạng của client.
     */
    private void closeResources() {
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
            LOGGER.warn("Lỗi khi đóng tài nguyên client: {}", exception.getMessage());
        }
    }
}
