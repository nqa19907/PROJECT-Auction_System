package com.auction.server.patterns.factory;
import com.auction.shared.model.Electronic;
import com.auction.shared.model.Item;

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
