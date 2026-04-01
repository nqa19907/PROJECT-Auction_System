package models;

public abstract class Item {
    public String itemname = "";
    private double startPrice=0;
    private String itemId="";


    public double getStartPrice() {
        return startPrice;
    }

    public abstract void getInfo();

    public String getItemId() {
        return itemId;
    }

    public Item(String itemName, String itemId, Double startPrice) {
        this.itemname = itemName;
        this.itemId = itemId;
        this.startPrice = startPrice;
    }

}
