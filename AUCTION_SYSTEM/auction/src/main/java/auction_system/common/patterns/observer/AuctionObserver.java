package auction_system.common.patterns.observer;

/**
 * Giao diện đại diện cho một người theo dõi (Observer) trong mẫu thiết kế Observer.
 */
public interface AuctionObserver {
    void update(String message);
}
