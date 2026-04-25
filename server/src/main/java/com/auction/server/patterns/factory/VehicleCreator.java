package com.auction.server.patterns.factory;
import com.auction.shared.model.Item;
import com.auction.shared.model.Vehicle;

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
