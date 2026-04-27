package auction_system.common.patterns.factory;
import auction_system.common.models.Electronic;
import auction_system.common.models.Item;

public class ElectronicCreator implements ItemCreator {
    @Override
    public Item createItem() {
        return new Electronic(
                "New Electronic Item",
                "Description here",
                0.0,
                "SYSTEM",
                "New",
                "none",
                "Generic Brand",
                0
        );
    }
}
