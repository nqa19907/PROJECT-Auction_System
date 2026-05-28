package auction_system.server.network.command.bidding;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.users.User;
import auction_system.common.network.Protocol;
import auction_system.server.core.AuctionManager;
import auction_system.server.network.command.Command;
import auction_system.server.session.ClientSession;
import java.util.Objects;

/**
 * Command cho phép user xóa phiên đấu giá của chính mình.
 *
 * <p>Luồng xử lý:
 * <ul>
 *   <li>Kiểm tra đăng nhập.</li>
 *   <li>Đọc mã phiên từ request.</li>
 *   <li>Kiểm tra phiên tồn tại.</li>
 *   <li>Kiểm tra quyền sở hữu phiên (owner).</li>
 *   <li>Tái sử dụng logic xóa sẵn có trong AuctionManager.</li>
 * </ul>
 */
public final class DeleteMyAuctionCommand implements Command {

    /** Vị trí tham số auctionId trong mảng request tách theo protocol. */
    private static final int IDX_AUCTION_ID = 1;

    /** Thành phần quản lý auction trung tâm của server. */
    private final AuctionManager auctionManager;

    /**
     * Khởi tạo command xóa phiên của user.
     *
     * @param auctionManager manager dùng để truy vấn/xóa phiên
     */
    public DeleteMyAuctionCommand(final AuctionManager auctionManager) {
        this.auctionManager = Objects.requireNonNull(auctionManager, "auctionManager");
    }

    /**
     * Xử lý lệnh DELETE_MY_AUCTION từ client.
     *
     * @param parts mảng request đã tách theo dấu phân cách protocol
     * @param session phiên làm việc của client đang gọi lệnh
     * @return response dạng text theo protocol DELETE_MY_AUCTION_OK/FAIL
     */
    @Override
    public String execute(final String[] parts, final ClientSession session) {
        // 1) Chặn sớm nếu client chưa đăng nhập.
        final User currentUser = (session != null) ? session.getCurrentUser() : null;
        if (currentUser == null) {
            return Protocol.Response.DELETE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn chưa đăng nhập.";
        }

        // 2) Kiểm tra request có auctionId hợp lệ hay không.
        if (parts.length <= IDX_AUCTION_ID || parts[IDX_AUCTION_ID].isBlank()) {
            return Protocol.Response.DELETE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Thiếu mã phiên đấu giá.";
        }

        final String auctionId = parts[IDX_AUCTION_ID].trim();

        // 3) Kiểm tra phiên có tồn tại không.
        final Auction auction = auctionManager.getAuctionById(auctionId);
        if (auction == null) {
            return Protocol.Response.DELETE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Không tìm thấy phiên để xóa.";
        }

        // 4) Chỉ chủ phiên (participant của phiên) mới được xóa.
        final boolean isOwner = auction.getParticipant() != null
                && currentUser.getId().equals(auction.getParticipant().getId());
        if (!isOwner) {
            return Protocol.Response.DELETE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Bạn không có quyền xóa phiên này.";
        }

        // 5) Tái sử dụng logic xóa đang dùng cho admin ở tầng core.
        final boolean deleted = auctionManager.deleteAuction(auctionId);
        if (!deleted) {
            return Protocol.Response.DELETE_MY_AUCTION_FAIL.name()
                    + Protocol.SEPARATOR + "Xóa phiên thất bại.";
        }

        return Protocol.Response.DELETE_MY_AUCTION_OK.name()
                + Protocol.SEPARATOR + auctionId;
    }
}