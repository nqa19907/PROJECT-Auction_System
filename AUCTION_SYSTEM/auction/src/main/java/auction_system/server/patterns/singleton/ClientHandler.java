package auction_system.server.patterns.singleton;

import auction_system.common.enums.AuctionStatus;
import auction_system.common.exceptions.AuctionClosedException;
import auction_system.common.exceptions.InvalidBidException;
import auction_system.common.models.Auction;
import auction_system.common.models.BidTransaction;
import auction_system.common.models.Bidder;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import auction_system.common.models.User;
import auction_system.common.patterns.observer.AuctionObserver;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
    private BufferedReader in;
    private PrintWriter out;

    /**
     * Người dùng đang đăng nhập trên kết nối này.
     * Null nếu chưa đăng nhập.
     */
    private User currentUser;

    /** ID các phiên đấu giá mà client này đang theo dõi (đã JOIN). */
    private final Set<String> joinedAuctionIds = new HashSet<>();

    /**
     * Khởi tạo handler cho một kết nối client.
     *
     * @param socket Socket kết nối từ client.
     */
    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    // =========================================================================
    // Runnable — vòng lặp đọc lệnh từ client
    // =========================================================================

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            out = new PrintWriter(socket.getOutputStream(), true);

            String line;
            while ((line = in.readLine()) != null) {
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
        String[] parts = rawCommand.split("\\|", -1);
        String command = parts[0].toUpperCase();

        switch (command) {
            case "LOGIN":
                handleLogin(parts);
                break;
            case "REGISTER":
                handleRegister(parts);
                break;
            case "LIST_AUCTIONS":
                handleListAuctions();
                break;
            case "GET_AUCTION":
                handleGetAuction(parts);
                break;
            case "JOIN_AUCTION":
                handleJoinAuction(parts);
                break;
            case "LEAVE_AUCTION":
                handleLeaveAuction(parts);
                break;
            case "PLACE_BID":
                handlePlaceBid(parts);
                break;
            case "LOGOUT":
                handleLogout();
                break;
            default:
                send("ERROR|Lệnh không hợp lệ: " + command);
                break;
        }
    }

    // =========================================================================
    // Xử lý từng lệnh
    // =========================================================================

    /**
     * Xử lý đăng nhập.
     * Lệnh:       {@code LOGIN|username|password}
     * Thành công: {@code LOGIN_OK|userId|username|role}
     * Thất bại:   {@code LOGIN_FAIL|message}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handleLogin(String[] parts) {
        if (parts.length < 3) {
            send("LOGIN_FAIL|Thiếu thông tin đăng nhập");
            return;
        }

        String username = parts[1];
        String password = parts[2];

        User user = AuctionManager.getInstance().findUserByCredentials(username, password);
        if (user == null) {
            send("LOGIN_FAIL|Tên đăng nhập hoặc mật khẩu không đúng");
            return;
        }

        if (AuctionManager.getInstance().isAlreadyOnline(user.getId())) {
            send("LOGIN_FAIL|Tài khoản này đang đăng nhập ở nơi khác");
            return;
        }

        currentUser = user;
        currentUser.setOnline(true);
        AuctionManager.getInstance().userLoggedIn(user);

        String role = getRoleName(user);
        send("LOGIN_OK|" + user.getId() + "|" + user.getUsername() + "|" + role);
        LOGGER.info("Đăng nhập: " + username + " [" + role + "]");
    }

    /**
     * Xử lý đăng ký tài khoản mới.
     * Lệnh:       {@code REGISTER|username|email|password|role}
     * Thành công: {@code REGISTER_OK}
     * Thất bại:   {@code REGISTER_FAIL|message}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handleRegister(String[] parts) {
        if (parts.length < 5) {
            send("REGISTER_FAIL|Thiếu thông tin đăng ký");
            return;
        }

        String username = parts[1];
        String email = parts[2];
        String password = parts[3];

        if (AuctionManager.getInstance().isUsernameTaken(username)) {
            send("REGISTER_FAIL|Tên đăng nhập đã tồn tại");
            return;
        }

        if (!email.contains("@")) {
            send("REGISTER_FAIL|Email không hợp lệ");
            return;
        }

        if (password.length() < 6) {
            send("REGISTER_FAIL|Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        String role = parts[4].toUpperCase();
        User newUser = "SELLER".equals(role)
                ? new Seller(username, email, password, 0.0, 5.0f)
                : new Bidder(username, email, password, 0.0);

        AuctionManager.getInstance().registerUser(newUser);
        send("REGISTER_OK");
        LOGGER.info("Đăng ký mới: " + username + " [" + role + "]");
    }

    /**
     * Trả về danh sách tất cả phiên đấu giá.
     * Lệnh:    {@code LIST_AUCTIONS}
     * Phản hồi: {@code AUCTION_LIST|n} sau đó n dòng dữ liệu.
     */
    private void handleListAuctions() {
        List<Auction> auctions = AuctionManager.getInstance().getAllAuctions();
        send("AUCTION_LIST|" + auctions.size());

        for (Auction auction : auctions) {
            Item item = auction.getItem();
            double currentPrice = (auction.getCurrentHighestBid() != null)
                    ? auction.getCurrentHighestBid().getAmount()
                    : item.getStartPrice();

            send(auction.getId()
                    + "|" + item.getItemName()
                    + "|" + currentPrice
                    + "|" + auction.getStatus().name()
                    + "|" + auction.getEndTime());
        }
    }

    /**
     * Trả về chi tiết một phiên đấu giá.
     * Lệnh: {@code GET_AUCTION|auctionId}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handleGetAuction(String[] parts) {
        if (parts.length < 2) {
            send("ERROR|Thiếu auctionId");
            return;
        }

        Auction auction = AuctionManager.getInstance().getAuctionById(parts[1]);
        if (auction == null) {
            send("ERROR|Không tìm thấy phiên đấu giá");
            return;
        }

        Item item = auction.getItem();
        double currentPrice = (auction.getCurrentHighestBid() != null)
                ? auction.getCurrentHighestBid().getAmount()
                : item.getStartPrice();

        send("AUCTION_DETAIL"
                + "|" + auction.getId()
                + "|" + item.getItemName()
                + "|" + item.getDescription()
                + "|" + item.getStartPrice()
                + "|" + currentPrice
                + "|" + auction.getStatus().name()
                + "|" + auction.getEndTime()
                + "|" + auction.getSeller().getUsername());
    }

    /**
     * Tham gia theo dõi một phiên đấu giá (đăng ký làm Observer).
     * Lệnh:       {@code JOIN_AUCTION|auctionId}
     * Thành công: {@code JOIN_OK|auctionId}
     * Thất bại:   {@code JOIN_FAIL|message}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handleJoinAuction(String[] parts) {
        if (!isLoggedIn()) {
            return;
        }
        if (parts.length < 2) {
            send("JOIN_FAIL|Thiếu auctionId");
            return;
        }

        String auctionId = parts[1];
        Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

        if (auction == null) {
            send("JOIN_FAIL|Không tìm thấy phiên đấu giá");
            return;
        }

        if (auction.getStatus() == AuctionStatus.FINISHED
                || auction.getStatus() == AuctionStatus.CANCELED) {
            send("JOIN_FAIL|Phiên đấu giá đã kết thúc hoặc bị huỷ");
            return;
        }

        auction.attach(this);
        joinedAuctionIds.add(auctionId);

        send("JOIN_OK|" + auctionId);
        LOGGER.info(currentUser.getUsername() + " tham gia phiên: " + auctionId);
    }

    /**
     * Rời khỏi phiên đấu giá (huỷ đăng ký Observer).
     * Lệnh:    {@code LEAVE_AUCTION|auctionId}
     * Phản hồi: {@code LEAVE_OK|auctionId}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handleLeaveAuction(String[] parts) {
        if (parts.length < 2) {
            send("ERROR|Thiếu auctionId");
            return;
        }

        String auctionId = parts[1];
        Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);

        if (auction != null) {
            auction.detach(this);
        }
        joinedAuctionIds.remove(auctionId);
        send("LEAVE_OK|" + auctionId);
    }

    /**
     * Đặt giá cho một phiên đấu giá.
     * Yêu cầu đã đăng nhập và là Bidder.
     * Lệnh:       {@code PLACE_BID|auctionId|amount}
     * Thành công: {@code BID_OK|auctionId|newPrice}
     * Thất bại:   {@code BID_FAIL|message}
     *
     * @param parts Mảng tham số từ lệnh đã tách.
     */
    private void handlePlaceBid(String[] parts) {
        if (!isLoggedIn()) {
            return;
        }

        if (!(currentUser instanceof Bidder)) {
            send("BID_FAIL|Chỉ người mua (Bidder) mới có thể đặt giá");
            return;
        }

        if (parts.length < 3) {
            send("BID_FAIL|Thiếu thông tin đặt giá");
            return;
        }

        String auctionId = parts[1];
        double amount;
        try {
            amount = Double.parseDouble(parts[2]);
        } catch (NumberFormatException ex) {
            send("BID_FAIL|Số tiền không hợp lệ");
            return;
        }

        if (amount <= 0) {
            send("BID_FAIL|Số tiền phải lớn hơn 0");
            return;
        }

        Auction auction = AuctionManager.getInstance().getAuctionById(auctionId);
        if (auction == null) {
            send("BID_FAIL|Không tìm thấy phiên đấu giá");
            return;
        }

        Bidder bidder = (Bidder) currentUser;
        BidTransaction bid = new BidTransaction(bidder, amount);

        try {
            auction.placeBid(bid);
            send("BID_OK|" + auctionId + "|" + amount);
            LOGGER.info(bidder.getUsername() + " đặt " + amount + " cho phiên " + auctionId);
        } catch (AuctionClosedException | InvalidBidException ex) {
            send("BID_FAIL|" + ex.getMessage());
        }
    }

    /**
     * Đăng xuất người dùng hiện tại.
     * Lệnh:    {@code LOGOUT}
     * Phản hồi: {@code LOGOUT_OK}
     */
    private void handleLogout() {
        if (currentUser != null) {
            AuctionManager.getInstance().userLoggedOut(currentUser);
            currentUser.setOnline(false);
            LOGGER.info("Đăng xuất: " + currentUser.getUsername());
            currentUser = null;
        }
        leaveAllAuctions();
        send("LOGOUT_OK");
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
        if (out != null && !socket.isClosed()) {
            out.println(message);
        }
    }

    /**
     * Kiểm tra client đã đăng nhập chưa.
     * Nếu chưa, tự động gửi thông báo lỗi về client.
     *
     * @return true nếu đã đăng nhập.
     */
    private boolean isLoggedIn() {
        if (currentUser == null) {
            send("ERROR|Bạn cần đăng nhập trước");
            return false;
        }
        return true;
    }

    /**
     * Xác định tên role của người dùng.
     *
     * @param user Người dùng cần kiểm tra.
     * @return Chuỗi "BIDDER", "SELLER", hoặc "ADMIN".
     */
    private String getRoleName(User user) {
        if (user instanceof auction_system.common.models.Admin) {
            return "ADMIN";
        } else if (user instanceof Seller) {
            return "SELLER";
        } else {
            return "BIDDER";
        }
    }

    /**
     * Detach khỏi tất cả phiên đang theo dõi.
     * Gọi khi client đăng xuất hoặc mất kết nối.
     */
    private void leaveAllAuctions() {
        new HashSet<>(joinedAuctionIds).forEach(id -> {
            Auction auction = AuctionManager.getInstance().getAuctionById(id);
            if (auction != null) {
                auction.detach(this);
            }
        });
        joinedAuctionIds.clear();
    }

    /**
     * Dọn dẹp tài nguyên khi client ngắt kết nối.
     * Luôn được gọi trong finally của {@link #run()}.
     */
    private void cleanup() {
        leaveAllAuctions();

        if (currentUser != null) {
            AuctionManager.getInstance().userLoggedOut(currentUser);
            currentUser.setOnline(false);
            LOGGER.info("Cleanup session: " + currentUser.getUsername());
            currentUser = null;
        }

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
        } catch (IOException ex) {
            LOGGER.warning("Lỗi khi đóng tài nguyên client: " + ex.getMessage());
        }
    }
}
