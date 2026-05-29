package auction_system.server.network.command.admin;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command cho phép ADMIN lấy danh sách toàn bộ phiên đấu giá qua socket.
 */
public class AdminListAuctionsCommand implements Command {
    private static final Logger LOGGER = LoggerFactory.getLogger(AdminListAuctionsCommand.class);
    private final AuctionManager auctionManager;

    public AdminListAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = auctionManager;
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        /*
         * Command admin tự kiểm tra quyền từ session hiện tại. Client có gửi được
         * lệnh hay không không quyết định quyền truy cập dữ liệu quản trị.
         */
        if (!isAdmin(session)) {
            return buildFailureResponse("Bạn không có quyền quản trị.");
        }

        /*
         * AuctionManager trả danh sách đã refresh lifecycle, nên status gửi cho
         * dashboard phản ánh thời gian hiện tại của server.
         */
        final List<Auction> auctions = auctionManager.getAllAuctions();
        return buildSuccessResponse(auctions);
    }

    private String buildSuccessResponse(final List<Auction> auctions) {
        final List<List<String>> auctionRecords = new ArrayList<>();
        for (Auction auction : auctions) {
            auctionRecords.add(toAuctionRecord(auction));
        }

        // Trả danh sách phiên admin bằng JSON, fallback về record string nếu serialize lỗi.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ADMIN_AUCTION_LIST.name(),
                            null,
                            "OK",
                            JsonProtocol.payloadOf(Map.of(
                                    "count", auctionRecords.size(),
                                    "auctions", auctionRecords)),
                            null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON response danh sách phiên admin: {}",
                    exception.getMessage());
            return buildStringSuccessResponse(auctionRecords);
        }
    }

    private List<String> toAuctionRecord(final Auction auction) {
        String itemName = auction.getItem() != null
                ? auction.getItem().getItemName()
                : "(Không có ten)";
        String seller = auction.getParticipant() != null
                ? auction.getParticipant().getUsername()
                : "(Không rõ)";
        String currentPrice = auction.getItem() != null
                ? String.valueOf(auction.getItem().getCurrentPrice())
                : "0";
        String status = auction.getStatus() != null
                ? auction.getStatus().name()
                : "UNKNOWN";

        // Giữ thứ tự field phiên giống protocol cũ để parser dashboard không đổi contract.
        return List.of(
                String.valueOf(auction.getId()),
                itemName,
                seller,
                currentPrice,
                status);
    }

    private String buildStringSuccessResponse(final List<List<String>> auctionRecords) {
        final StringBuilder response = new StringBuilder();
        response.append(Protocol.Response.ADMIN_AUCTION_LIST.name())
                .append(Protocol.SEPARATOR).append(auctionRecords.size());

        for (List<String> auctionRecord : auctionRecords) {
            response.append(Protocol.RECORD_SEPARATOR)
                    .append(String.join(Protocol.SEPARATOR, auctionRecord));
        }

        return response.toString();
    }

    private String buildFailureResponse(final String message) {
        // Trả lỗi quyền/tải phiên bằng JSON để client đọc message thống nhất.
        try {
            return JsonProtocol.stringify(
                    new JsonMessage(
                            Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name(),
                            null,
                            "FAIL",
                            null,
                            message));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON lỗi danh sách phiên admin: {}",
                    exception.getMessage());
            return Protocol.Response.ADMIN_AUCTION_LIST_FAIL.name()
                    + Protocol.SEPARATOR
                    + message;
        }
    }

    private boolean isAdmin(final ClientSession session) {
        // Role lấy từ user trong session socket, không tin dữ liệu role do client gửi lên.
        final User currentUser = session.getCurrentUser();
        return currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRoleName());
    }
}
