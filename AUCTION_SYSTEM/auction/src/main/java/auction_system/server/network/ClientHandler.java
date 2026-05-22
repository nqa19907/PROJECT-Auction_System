package auction_system.server.network;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.Participant;
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
import auction_system.server.network.command.PublishItemCommand;
import auction_system.server.network.command.RegisterCommand;
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
import java.util.logging.Logger;

/**
 * Xử lý giao tiếp với một client duy nhất.
 *
 * <p>Mỗi ClientHandler chạy trên một thread riêng và đồng thời đóng vai trò
 * {@link AuctionObserver}. Khi phiên đấu giá có thay đổi, {@link Auction}
 * sẽ gọi {@link #update(String)} để đẩy thông báo realtime về client.
 */
public class ClientHandler implements Runnable, AuctionObserver {

    private static final Logger logger = Logger.getLogger(ClientHandler.class.getName());

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
        this.session = new ClientSession(this, auctionManager);
        this.participantItemService = participantItemService;
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
                        Protocol.Command.JOIN_AUCTION.name(),
                        new JoinAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.LEAVE_AUCTION.name(),
                        new LeaveAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.PLACE_BID.name(),
                        new PlaceBidCommand(auctionBidService)),
                Map.entry(
                        Protocol.Command.LOGOUT.name(),
                        new LogoutCommand(auctionManager)));
                Map.entry(
                        Protocol.Command.PUBLISH_ITEM.name(), 
                        new PublishItemCommand(participantItemService, auctionManager));
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
            logger.info("Client ngắt kết nối đột ngột: " + socket.getInetAddress());
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
     * Gửi một dòng dữ liệu về client qua socket.
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
        session.leaveAllAuctions();

        final User currentUser = session.getCurrentUser();
        if (currentUser != null) {
            auctionManager.userLoggedOut(currentUser);
            currentUser.setOnline(false);
            logger.info("Dọn dẹp phiên làm việc của: " + currentUser.getUsername());
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
            logger.warning("Lỗi khi đóng tài nguyên client: " + exception.getMessage());
        }
    }
}