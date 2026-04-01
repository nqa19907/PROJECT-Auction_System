package models;

public abstract class Item {
    public String itemname = "";
    private double startPrice = 0;
    private double currentPrice = 0;
    private String itemId = "";

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getItemId() {
        return itemId;
    }

    public String getItemname() {
        return itemname;
    }

    public double getStartPrice() {
        return startPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public void setItemId(String itemId) {
        this.itemId = itemId;
    }

    public void setItemname(String itemname) {
        this.itemname = itemname;
    }

    public void setStartPrice(double startPrice) {
        this.startPrice = startPrice;
    }

    public Item(String itemName, String itemId, Double startPrice) {
        this.itemname = itemName;
        this.itemId = itemId;
        this.startPrice = startPrice;
    }

    @Override
    public String toString(){
        return "itemId: " + getItemId() + ", itemName: " + getItemId() + ", currentPrice: " + getCurrentPrice();
    }

}
