package auction_system.server.patterns.singleton;

import auction_system.common.models.Auction;
import auction_system.common.models.User;

import java.util.List;
import java.util.Map;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class AuctionManager {
    private static AuctionManager instance;
    private List<Auction> auctionList;
    private Map<String, User> activeUsers;

    private AuctionManager() {
        this.auctionList = new ArrayList<>();
        this.activeUsers = new ConcurrentHashMap<>(); // Dùng ConcurrentHashMap để an toàn trong môi trường đa luồng
    }

    public static synchronized AuctionManager getInstance() {
        if (instance == null) {
            instance = new AuctionManager();
        }

        return instance;
    }
}
