package models;

public class Electronic extends Item {
    String electronicBrand;

    public Electronic(String itemName, String itemId, Double startPrice, String electronicBrand) {
        super(itemName, itemId, startPrice);
        this.electronicBrand = electronicBrand;
    }
    public String getElectronicBrand() {
        return electronicBrand;
    }
    public void setElectronicBrand(String electronicBrand) {
        this.electronicBrand = electronicBrand;
    }

    @Override
    public String toString() {
        return "itemId: " + getItemId() + ", itemName: " + getItemId() + ", currentPrice: " + getCurrentPrice() + ", brand:" + getElectronicBrand();
    }
}
