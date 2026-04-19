package auction_system.server.patterns.factory;

import auction_system.common.models.Art;
import auction_system.common.models.Item;

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
