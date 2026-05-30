package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.util.Map;
import java.util.Objects;

/**
 * Command cho phép user xóa phiên đấu giá của chính mình.
 */
public final class DeleteMyAuctionCommand implements Command {

    private static final int IDX_AUCTION_ID = 1;
    private final AuctionManager auctionManager;

    public DeleteMyAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // Chặn request chưa đăng nhập hoặc thiếu mã phiên trước khi truy cập dữ liệu.
        final User currentUser = session == null ? null : session.getCurrentUser();
        if (currentUser == null) {
            return failure("Bạn chưa đăng nhập.");
        }
        if (parts.length <= IDX_AUCTION_ID || parts[IDX_AUCTION_ID].isBlank()) {
            return failure("Thiếu mã phiên đấu giá.");
        }

        // Chỉ cho phép chủ phiên xóa auction thuộc tài khoản hiện tại.
        final String auctionId = parts[IDX_AUCTION_ID].trim();
        final Auction auction = auctionManager.getAuctionById(auctionId);
        if (auction == null) {
            return failure("Không tìm thấy phiên để xóa.");
        }
        if (auction.getParticipant() == null
                || !currentUser.getId().equals(auction.getParticipant().getId())) {
            return failure("Bạn không có quyền xóa phiên này.");
        }
        if (!auctionManager.deleteAuction(auctionId)) {
            return failure("Xóa phiên thất bại.");
        }

        // Trả mã phiên đã xóa để client cập nhật lại bảng quản lý.
        return response(
                Protocol.Response.DELETE_MY_AUCTION_OK,
                "OK",
                Map.of("auctionId", auctionId),
                "Xóa phiên thành công.");
    }

    private String failure(final String message) {
        // Chuẩn hóa mọi nhánh lỗi về cùng JSON response type.
        return response(Protocol.Response.DELETE_MY_AUCTION_FAIL, "FAIL", null, message);
    }

    private String response(
            final Protocol.Response type,
            final String status,
            final Object payload,
            final String message) {
        // Đóng gói response thành JSON một dòng để gửi qua socket.
        return JsonProtocol.stringifyRequired(new JsonMessage(
                type.name(),
                null,
                status,
                payload == null ? null : JsonProtocol.payloadOf(payload),
                message));
    }
}
