package auction_system.common.models;

import java.util.ArrayList;
import java.util.List;

public class Seller extends Participant {
    private float rating;
    private List<Item> managedItems;

    public Seller(String username, String email, String password, double balance,
                  float rating) {
        super(username, email, password, balance);
        this.rating = rating;
        managedItems = new ArrayList<>();
    }

    @Override
    public void displayDashboard() {
        // to be coded
    }

    public void createAuction (Item item) {
        // to be coded
    }

    public void updateAuction(Item item) {
        // to be coded
    }

    public void deleteAuction(Item item) {
        // to be coded
    }

    public void endAuction(Item item) {
        // to be coded
    }

    public float getRating() {
        return rating;
    }

    public void setRating(float rating) {
        this.rating = rating;
    }


}
