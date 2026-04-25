package com.auction.server.patterns.factory;
import com.auction.shared.model.Art;
import com.auction.shared.model.Item;

public class ArtCreator implements ItemCreator {
    @Override
    public Item createItem() {
        return new Art(
                "New Art Piece",
                "Description here",
                0.0,
                "SYSTEM",
                "Excellent",
                "none",
                "Unknown Artist",
                "Unknown Year",
                false);
    }
}
