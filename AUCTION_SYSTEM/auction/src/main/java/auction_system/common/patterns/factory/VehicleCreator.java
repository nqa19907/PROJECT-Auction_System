package auction_system.common.patterns.factory;
import auction_system.common.models.Item;
import auction_system.common.models.Vehicle;

public class VehicleCreator implements ItemCreator {
    @Override
    public Item createItem() {
        return new Vehicle(
                "New Vehicle",
                "Description here",
                0.0,
                "SYSTEM",
                "Used",
                "none",
                "Generic Make",
                "Generic Model",
                0,
                0);
    }
}
