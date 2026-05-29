package auction_system.server.core;

import auction_system.common.models.auctions.Auction;
import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.auctions.BidTransaction;
import auction_system.common.models.users.Participant;
import auction_system.common.network.JsonMessage;
import auction_system.common.network.JsonProtocol;
import auction_system.common.network.Protocol;
import auction_system.server.persistence.serialization.SerializedDatabase;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gom toàn bộ realtime notification liên quan tới auction/user online.
 */
final class AuctionRealtimeNotifier {
    private static final Logger LOGGER = LoggerFactory.getLogger(AuctionRealtimeNotifier.class);

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

        final String message = buildBalanceUpdatedMessage(participant.getBalance());
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

        final String message = buildAuctionWinnerMessage(auctionId, itemName);
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

        final String message = buildAuctionLostMessage(auctionId, itemName, winnerUsername);
        loserObserver.update(message);
    }

    private String buildBalanceUpdatedMessage(final double balance) {
        // Gửi số dư realtime bằng JSON, fallback về BALANCE_UPDATED|balance nếu lỗi.
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    Protocol.Response.BALANCE_UPDATED.name(),
                    null,
                    "OK",
                    JsonProtocol.payloadOf(Map.of("balance", balance)),
                    null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON BALANCE_UPDATED: {}", exception.getMessage());
            return Protocol.Response.BALANCE_UPDATED.name()
                    + Protocol.SEPARATOR
                    + balance;
        }
    }

    private String buildAuctionWinnerMessage(final String auctionId, final String itemName) {
        // Gửi thông báo winner bằng JSON, fallback về protocol string cũ nếu lỗi.
        final String safeItemName = itemName == null ? "" : itemName;
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    Protocol.Response.AUCTION_WINNER.name(),
                    null,
                    "OK",
                    JsonProtocol.payloadOf(Map.of(
                            "auctionId", auctionId,
                            "itemName", safeItemName)),
                    null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON AUCTION_WINNER: {}", exception.getMessage());
            return Protocol.Response.AUCTION_WINNER.name()
                    + Protocol.SEPARATOR
                    + auctionId
                    + Protocol.SEPARATOR
                    + safeItemName;
        }
    }

    private String buildAuctionLostMessage(
            final String auctionId,
            final String itemName,
            final String winnerUsername) {
        // Gửi thông báo loser bằng JSON, fallback về protocol string cũ nếu lỗi.
        final String safeItemName = itemName == null ? "" : itemName;
        final String safeWinnerUsername = winnerUsername == null ? "NONE" : winnerUsername;
        try {
            return JsonProtocol.stringify(new JsonMessage(
                    Protocol.Response.AUCTION_LOST.name(),
                    null,
                    "OK",
                    JsonProtocol.payloadOf(Map.of(
                            "auctionId", auctionId,
                            "itemName", safeItemName,
                            "winnerUsername", safeWinnerUsername)),
                    null));
        } catch (JsonProcessingException exception) {
            LOGGER.warn("Không tạo được JSON AUCTION_LOST: {}", exception.getMessage());
            return Protocol.Response.AUCTION_LOST.name()
                    + Protocol.SEPARATOR
                    + auctionId
                    + Protocol.SEPARATOR
                    + safeItemName
                    + Protocol.SEPARATOR
                    + safeWinnerUsername;
        }
    }
}
