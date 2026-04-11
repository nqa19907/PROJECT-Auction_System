package auction_system.server.patterns.factory;

import auction_system.common.models.Item;

public interface ItemCreator {
    public Item createItem();
}
