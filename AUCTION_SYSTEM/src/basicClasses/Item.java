package basicClasses;
public abstract class Item  {
    public String itemName;
    private double startPrice;
    private String itemId;
    public double getStartPrice() {
        return startPrice;
    }
    public abstract void getInfo();
    public String getItemId() {
        return itemId;
    }
    public Item(String itemName, String itemId, Double startPrice){
        this.itemName = itemName;
        this.itemId = itemId;
        this.startPrice = startPrice;
    }

}
