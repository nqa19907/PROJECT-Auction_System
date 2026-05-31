package auction_system.server.network;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.network.command.JsonPayloadCommand;
import auction_system.server.network.command.admin.AdminCancelAuctionCommand;
import auction_system.server.network.command.admin.AdminDeleteAuctionCommand;
import auction_system.server.network.command.admin.AdminDeleteUserCommand;
import auction_system.server.network.command.admin.AdminListAuctionsCommand;
import auction_system.server.network.command.admin.AdminListUsersCommand;
import auction_system.server.network.command.auction.GetAuctionCommand;
import auction_system.server.network.command.auction.JoinAuctionCommand;
import auction_system.server.network.command.auction.LeaveAuctionCommand;
import auction_system.server.network.command.auction.ListAuctionsCommand;
import auction_system.server.network.command.auth.LoginCommand;
import auction_system.server.network.command.auth.LogoutCommand;
import auction_system.server.network.command.auth.RegisterCommand;
import auction_system.server.network.command.bidding.AutoBidCommand;
import auction_system.server.network.command.bidding.DeleteMyAuctionCommand;
import auction_system.server.network.command.bidding.DisableAutoBidCommand;
import auction_system.server.network.command.bidding.GetAutoBidStatusCommand;
import auction_system.server.network.command.bidding.GetBidHistoryCommand;
import auction_system.server.network.command.bidding.ListMyAuctionsCommand;
import auction_system.server.network.command.bidding.PlaceBidCommand;
import auction_system.server.network.command.bidding.PublishItemCommand;
import auction_system.server.network.command.bidding.SetAntiSnipingCommand;
import auction_system.server.network.command.bidding.UpdateMyAuctionCommand;
import auction_system.server.network.command.wallet.DepositCommand;
import auction_system.server.services.auction.ParticipantItemService;
import auction_system.server.services.auth.AuthService;
import auction_system.server.services.autobid.AutoBidService;
import auction_system.server.services.bidding.AuctionBidService;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
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
    private final AutoBidService autoBidService;
    private final AuctionBidService auctionBidService;
    private final ParticipantItemService participantItemService;
    private final Map<String, Object> commandMap;
    private final ClientSession session;

    private BufferedReader inputReader;
    private PrintWriter outputWriter;

    /**
     * Khởi tạo handler cho một kết nối client.
     *
     * @param socket socket kết nối từ client
     * @param auctionManager manager quản lý phiên đấu giá và trạng thái online
     * @param authService service xác thực tài khoản bằng database
     * @param autoBidService service quản lý cấu hình auto-bid
     * @param auctionBidService service xử lý đặt giá
     * @param participantItemService service xử lý item người dùng
     */
    public ClientHandler(
            final Socket socket,
            final AuctionManager auctionManager,
            final AuthService authService,
            final AutoBidService autoBidService,
            final AuctionBidService auctionBidService,
            final ParticipantItemService participantItemService) {
        this.socket = Objects.requireNonNull(socket, "socket");
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
        this.authService = Objects.requireNonNull(authService, "authService");
        this.auctionBidService = Objects.requireNonNull(auctionBidService, "auctionBidService");
        this.autoBidService = autoBidService;
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
    private Map<String, Object> createCommandMap() {
        return Map.ofEntries(
                Map.entry(
                        Protocol.Command.LOGIN.name(),
                        new LoginCommand(authService, auctionManager)),
                Map.entry(
                        Protocol.Command.REGISTER.name(),
                        new RegisterCommand(authService, auctionManager)),
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
                        Protocol.Command.JOIN_AUCTION.name(),
                        new JoinAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.LEAVE_AUCTION.name(),
                        new LeaveAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.PLACE_BID.name(),
                        new PlaceBidCommand(auctionBidService)),
                Map.entry(
                        Protocol.Command.ENABLE_AUTO_BID.name(),
                        new AutoBidCommand(autoBidService, auctionBidService)),
                Map.entry(
                        Protocol.Command.DISABLE_AUTO_BID.name(),
                        new DisableAutoBidCommand(autoBidService)),
                Map.entry(
                        Protocol.Command.GET_AUTO_BID.name(),
                        new GetAutoBidStatusCommand(autoBidService)),
                Map.entry(
                        Protocol.Command.SET_ANTI_SNIPING.name(),
                        new SetAntiSnipingCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.DEPOSIT.name(),
                        new DepositCommand(authService, auctionBidService)),
                Map.entry(
                        Protocol.Command.LOGOUT.name(),
                        new LogoutCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.PUBLISH_ITEM.name(),
                        new PublishItemCommand(participantItemService, auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_CANCEL_AUCTION.name(),
                        new AdminCancelAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_DELETE_AUCTION.name(),
                        new AdminDeleteAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_DELETE_USER.name(),
                        new AdminDeleteUserCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_LIST_USERS.name(),
                        new AdminListUsersCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.ADMIN_LIST_AUCTIONS.name(),
                        new AdminListAuctionsCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.LIST_MY_AUCTIONS.name(),
                        new ListMyAuctionsCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.DELETE_MY_AUCTION.name(),
                        new DeleteMyAuctionCommand(auctionManager)),
                Map.entry(
                        Protocol.Command.UPDATE_MY_AUCTION.name(),
                        new UpdateMyAuctionCommand(auctionManager))
        );
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
        final JsonMessage message = parseMessage(rawCommand);
        if (message == null || message.command() == null || message.command().isBlank()) {
            send(buildErrorResponse("Lệnh JSON không hợp lệ."));
            return;
        }

        final String commandName = message.command().toUpperCase();
        final Object command = commandMap.get(commandName);

        // Từ chối command không đăng ký trước khi gọi xử lý nghiệp vụ.
        if (command == null) {
            send(buildErrorResponse("Lệnh không hợp lệ: " + commandName));
            return;
        }

        // Mỗi command trả tối đa một JSON response một dòng.
        final String response = executeCommand(command, message);
        if (response != null) {
            send(response);
        }
    }

    private String executeCommand(final Object command, final JsonMessage message) {
        if (command instanceof JsonPayloadCommand jsonPayloadCommand) {
            return jsonPayloadCommand.execute(message.payload(), session);
        }

        if (command instanceof Command legacyCommand) {
            return legacyCommand.execute(parseCommandParts(message), session);
        }

        throw new IllegalStateException("Command handler không được hỗ trợ: "
                + command.getClass().getName());
    }

    private JsonMessage parseMessage(final String rawCommand) {
        try {
            return JsonProtocol.parse(rawCommand);
        } catch (IOException exception) {
            LOGGER.warn("Không parse được JSON request: {}", exception.getMessage());
            return null;
        }
    }

    /**
     * Chuyển request JSON thành mảng parts cho command legacy hiện tại.
     *
     * @param message request đã parse từ client
     * @return parts tương thích với command layer hiện tại
     */
    private String[] parseCommandParts(final JsonMessage message) {
        // Phần tử đầu là command; payload object được map theo field từng command.
        final List<String> parts = new ArrayList<>();
        parts.add(message.command());

        if (message.payload() == null || message.payload().isNull()) {
            return parts.toArray(String[]::new);
        }

        if (message.payload().isObject()) {
            appendObjectPayloadParts(message.command(), message.payload(), parts);
        } else if (message.payload().isArray()) {
            message.payload().forEach(value -> parts.add(value.asText()));
        } else if (message.payload().isValueNode()) {
            parts.add(message.payload().asText());
        }

        return parts.toArray(String[]::new);
    }

    private void appendObjectPayloadParts(
            final String command,
            final JsonNode payload,
            final List<String> parts) {
        if (command == null) {
            return;
        }

        switch (command.toUpperCase()) {
            case "LOGIN" -> {
                addPayloadField(payload, parts, "email");
                addPayloadField(payload, parts, "password");
            }
            case "REGISTER" -> {
                addPayloadField(payload, parts, "username");
                addPayloadField(payload, parts, "email");
                addPayloadField(payload, parts, "password");
                addPayloadField(payload, parts, "roleName");
            }
            case "PLACE_BID" -> {
                addPayloadField(payload, parts, "auctionId");
                addPayloadField(payload, parts, "amount");
            }
            case "ENABLE_AUTO_BID" -> {
                addPayloadField(payload, parts, "auctionId");
                addPayloadField(payload, parts, "maxAmount");
                addPayloadField(payload, parts, "stepAmount");
            }
            case "SET_ANTI_SNIPING" -> {
                addPayloadField(payload, parts, "auctionId");
                addPayloadField(payload, parts, "enabled");
            }
            case "DEPOSIT" -> addPayloadField(payload, parts, "amount");
            case "ADMIN_DELETE_USER" -> addPayloadField(payload, parts, "userId");
            case "ADMIN_DELETE_AUCTION", "ADMIN_CANCEL_AUCTION" ->
                    addPayloadField(payload, parts, "auctionId");
            case "PUBLISH_ITEM" -> {
                addPayloadField(payload, parts, "category");
                addPayloadField(payload, parts, "itemName");
                addPayloadField(payload, parts, "description");
                addPayloadField(payload, parts, "condition");
                addPayloadField(payload, parts, "startPrice");
                addPayloadField(payload, parts, "startTime");
                addPayloadField(payload, parts, "endTime");
                addPayloadField(payload, parts, "imagePath");
                addPayloadField(payload, parts, "antiSnipingEnabled");
            }
            case "UPDATE_MY_AUCTION" -> {
                addPayloadField(payload, parts, "auctionId");
                addPayloadField(payload, parts, "category");
                addPayloadField(payload, parts, "itemName");
                addPayloadField(payload, parts, "description");
                addPayloadField(payload, parts, "condition");
                addPayloadField(payload, parts, "endTime");
                addPayloadField(payload, parts, "imagePath");
            }
            default -> {
                if (payload.has("auctionId")) {
                    addPayloadField(payload, parts, "auctionId");
                }
            }
        }
    }

    private void addPayloadField(
            final JsonNode payload,
            final List<String> parts,
            final String fieldName) {
        parts.add(payload.path(fieldName).asText());
    }

    private String buildErrorResponse(final String message) {
        try {
            // Đóng gói lỗi dispatch bằng JSON để client route qua ERROR.
            return JsonProtocol.stringify(new JsonMessage(
                    Protocol.Response.ERROR.name(),
                    null,
                    "FAIL",
                    null,
                    message));
        } catch (IOException exception) {
            LOGGER.warn("Không tạo được JSON ERROR: {}", exception.getMessage());
            throw new IllegalStateException("Không tạo được JSON ERROR.", exception);
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
