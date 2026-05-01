package auction_system.server.patterns.singleton;

import auction_system.common.models.Auction;
import auction_system.common.models.Item;
import auction_system.common.models.Seller;
import auction_system.common.models.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class AuctionManager {
    private static AuctionManager instance;
    private final List<Auction> auctionList;
    private final Map<String, User> activeUsers;

    private AuctionManager() {
        // Sử dụng list thread-safe để tránh lỗi khi nhiều client cùng tương tác
        this.auctionList = new CopyOnWriteArrayList<>();
        // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
        this.activeUsers = new ConcurrentHashMap<>();
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }

        return instance;
    }

    // --- Quản lý phiên đấu giá ---
    public void createAuction(Item item, Seller seller, LocalDateTime startTime, LocalDateTime endTime) {
        Auction newAuction = new Auction(item, seller, startTime, endTime);
        auctionList.add(newAuction);
        // Có thể thêm logic thông báo cho mọi người về phiên đấu giá
        // to be coded
    }

    public Auction getAuctionById(String auctionId) {
        return auctionList.stream()
                .filter(auction -> auction.getId().equals(auctionId))
                .findFirst()
                .orElse(null);
    }

    public List<Auction> getAllAuctions() {
        // Trả về một view không thể sửa đổi của danh sách để bên
        // ngoài không thể thay đổi trực tiếp
        return Collections.unmodifiableList(auctionList);
    }

    // -- Quản lý người dùng --
    public void userLoggedIn(User user) {
        activeUsers.put(user.getId(), user);
    }
    public void userLoggedOut(User user) {
        activeUsers.remove(user.getId());
    }
}
