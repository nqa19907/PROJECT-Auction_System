package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Command trả về danh sách phiên đấu giá do user hiện tại đăng bán.
 */
public final class ListMyAuctionsCommand implements Command {

    private static final String CONDITION_PREFIX = "\nTình trạng: ";
    private final AuctionManager auctionManager;

    public ListMyAuctionsCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // Danh sách cá nhân chỉ được truy vấn sau khi session đã đăng nhập.
        if (session == null || session.getCurrentUser() == null) {
            return response(Protocol.Response.ERROR, "FAIL", null, "Chưa đăng nhập.");
        }

        // Lọc auction theo participant sở hữu và chuyển từng phiên thành JSON row.
        final String currentUserId = session.getCurrentUser().getId();
        final List<Map<String, Object>> rows = auctionManager.getAllAuctions().stream()
                .filter(auction -> auction.getParticipant() != null)
                .filter(auction -> currentUserId.equals(auction.getParticipant().getId()))
                .map(this::toRow)
                .toList();

        return response(
                Protocol.Response.MY_AUCTION_LIST,
                "OK",
                Map.of("auctions", rows),
                null);
    }

    private Map<String, Object> toRow(final Auction auction) {
        // Ưu tiên giá bid cao nhất; nếu chưa có bid thì dùng giá hiện tại của item.
        final double currentPrice = auction.getCurrentHighestBid() != null
                ? auction.getCurrentHighestBid().getAmount()
                : auction.getItem().getCurrentPrice();
        final String[] description = extractDescriptionAndCondition(
                safe(auction.getItem().getDescription()));

        // Giữ tên field ổn định để client parse trực tiếp theo JSON key.
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", String.valueOf(auction.getId()));
        row.put("itemName", safe(auction.getItem().getItemName()));
        row.put("currentPrice", String.valueOf(currentPrice));
        row.put("status", String.valueOf(auction.getStatus()));
        row.put("startTime", String.valueOf(auction.getStartTime()));
        row.put("endTime", String.valueOf(auction.getEndTime()));
        row.put("category", safe(auction.getItem().getCategory()));
        row.put("description", description[0]);
        row.put("condition", description[1]);
        return row;
    }

    private String[] extractDescriptionAndCondition(final String fullDescription) {
        // Tách tình trạng được lưu kèm mô tả để trả lại hai field riêng cho form sửa.
        final int index = fullDescription.lastIndexOf(CONDITION_PREFIX);
        if (index < 0) {
            return new String[]{fullDescription, ""};
        }
        return new String[]{
            fullDescription.substring(0, index),
            fullDescription.substring(index + CONDITION_PREFIX.length())
        };
    }

    private String safe(final String value) {
        return value == null ? "" : value.trim();
    }

    private String response(
            final Protocol.Response type,
            final String status,
            final Object payload,
            final String message) {
        // Đóng gói danh sách hoặc lỗi thành JSON response một dòng.
        return JsonProtocol.stringifyRequired(new JsonMessage(
                type.name(),
                null,
                status,
                payload == null ? null : JsonProtocol.payloadOf(payload),
                message));
    }
}
