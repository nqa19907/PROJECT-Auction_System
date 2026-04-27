package auction_system.common.patterns.factory;
import auction_system.common.models.Item;

public interface ItemCreator {
    public Item createItem();
}
