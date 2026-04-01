package models;

public class Art extends Item {
    String author;

    public Art(String itemName, String itemId, Double startPrice, String author) {
        super(itemName, itemId, startPrice);
        this.author = author;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    @Override
    public String toString() {
        return "itemId: " + getItemId() + ", itemName: " + getItemId() + ", currentPrice: " + getCurrentPrice()
                + ", author:" + getAuthor();
    }

}
