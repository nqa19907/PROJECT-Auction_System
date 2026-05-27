package auction_system.server.core;

import auction_system.common.models.auctions.AuctionObserver;
import auction_system.common.models.users.User;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry runtime cho user đang online và kênh observer của họ.
 */
final class OnlineUserRegistry {

    private final Map<String, User> activeUsers;
    private final Map<String, AuctionObserver> activeUserObservers;

    OnlineUserRegistry() {
        this.activeUsers = new ConcurrentHashMap<>();
        this.activeUserObservers = new ConcurrentHashMap<>();
    }

    void userLoggedIn(final User user) {
        activeUsers.put(user.getId(), user);
    }

    void userLoggedIn(final User user, final AuctionObserver observer) {
        userLoggedIn(user);
        if (observer != null) {
            activeUserObservers.put(user.getId(), observer);
        }
    }

    void userLoggedOut(final User user) {
        activeUsers.remove(user.getId());
        activeUserObservers.remove(user.getId());
    }

    void remove(final User user) {
        activeUsers.remove(user.getId());
        activeUserObservers.remove(user.getId());
    }

    boolean isAlreadyOnline(final String userId) {
        return activeUsers.containsKey(userId);
    }

    int getOnlineCount() {
        return activeUsers.size();
    }

    Map<String, User> getActiveUsers() {
        return Collections.unmodifiableMap(activeUsers);
    }

    AuctionObserver getObserver(final String userId) {
        return activeUserObservers.get(userId);
    }

    Iterable<AuctionObserver> getObservers() {
        return activeUserObservers.values();
    }
}
