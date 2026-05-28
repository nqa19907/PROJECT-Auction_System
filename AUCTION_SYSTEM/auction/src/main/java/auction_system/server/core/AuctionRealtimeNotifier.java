package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.network.Protocol;
import auction_system.server.persistence.serialization.SerializedDatabase;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Gom toàn bộ realtime notification liên quan tới auction/user online.
 */
final class AuctionRealtimeNotifier {

    private final SerializedDatabase database;
    private final OnlineUserRegistry onlineUsers;

    AuctionRealtimeNotifier(
            final SerializedDatabase database,
            final OnlineUserRegistry onlineUsers) {
        this.database = Objects.requireNonNull(database, "database");
        this.onlineUsers = Objects.requireNonNull(onlineUsers, "onlineUsers");
    }

    void notifyAuctionCreated(final Auction auction) {
        // Gói thông báo tối giản: client sẽ dùng auctionId để tự tải chi tiết.
        final String message = Protocol.Response.AUCTION_CREATED.name()
                + Protocol.SEPARATOR
                + auction.getId();
        onlineUsers.getObservers().forEach(observer -> observer.update(message));
    }

    void notifyAntiSnipingUpdated(final Auction auction) {
        final String message = Protocol.Response.ANTI_SNIPING_UPDATED.name()
                + Protocol.SEPARATOR + auction.getId()
                + Protocol.SEPARATOR + auction.isAntiSnipingEnabled();
        auction.notifyObservers(message);
    }

    void notifyBalanceUpdated(final Participant participant) {
        if (participant == null) {
            return;
        }

        // Chỉ gửi realtime nếu user còn kết nối socket đang active.
        final AuctionObserver observer = onlineUsers.getObserver(participant.getId());
        if (observer == null) {
            return;
        }

        // Client chỉ cần số dư mới để cập nhật UI ví.
        final String message = Protocol.Response.BALANCE_UPDATED.name()
                + Protocol.SEPARATOR
                + participant.getBalance();
        observer.update(message);
    }

    void notifyAuctionResult(final Auction auction) {
        // Không có highest bid thì phiên kết thúc mà không có winner/loser.
        final BidTransaction highestBid = auction.getCurrentHighestBid();
        if (highestBid == null || highestBid.getParticipant() == null) {
            return;
        }

        // Tách thông báo riêng cho winner và các bidder còn lại.
        final Participant winner = highestBid.getParticipant();
        final String itemName = auction.getItem() != null
                ? auction.getItem().getItemName()
                : "";
        notifyWinner(winner, auction.getId(), itemName);
        notifyLosers(auction, winner, itemName);
    }

    private void notifyWinner(
            final Participant winner,
            final String auctionId,
            final String itemName) {
        // Winner offline thì bỏ qua, lúc vào lại client sẽ đọc trạng thái từ database.
        final AuctionObserver winnerObserver = onlineUsers.getObserver(winner.getId());
        if (winnerObserver == null) {
            return;
        }

        // Thông báo winner chứa itemName để UI hiển thị popup ngay.
        final String message = Protocol.Response.AUCTION_WINNER.name()
                + Protocol.SEPARATOR
                + auctionId
                + Protocol.SEPARATOR
                + itemName;
        winnerObserver.update(message);
    }

    private void notifyLosers(
            final Auction auction,
            final Participant winner,
            final String itemName) {
        final Set<String> notifiedLoserIds = new HashSet<>();

        // Lấy tất cả bidder của phiên, loại winner và tránh gửi trùng cho cùng user.
        database.bidTransactions()
                .findByAuctionId(auction.getId())
                .stream()
                .map(BidTransaction::getParticipant)
                .filter(Objects::nonNull)
                .filter(participant -> !participant.getId().equals(winner.getId()))
                .filter(participant -> notifiedLoserIds.add(participant.getId()))
                .forEach(participant -> notifyAuctionLost(
                        participant,
                        auction.getId(),
                        itemName,
                        winner.getUsername()));
    }

    private void notifyAuctionLost(
            final Participant loser,
            final String auctionId,
            final String itemName,
            final String winnerUsername) {
        // Loser offline thì bỏ qua thông báo realtime.
        final AuctionObserver loserObserver = onlineUsers.getObserver(loser.getId());
        if (loserObserver == null) {
            return;
        }

        // Gửi đủ ngữ cảnh để client hiển thị tên vật phẩm và người thắng.
        final String message = Protocol.Response.AUCTION_LOST.name()
                + Protocol.SEPARATOR
                + auctionId
                + Protocol.SEPARATOR
                + itemName
                + Protocol.SEPARATOR
                + winnerUsername;
        loserObserver.update(message);
    }
}
