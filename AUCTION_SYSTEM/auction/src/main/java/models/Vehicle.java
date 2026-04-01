package models;
public class Vehicle extends Item {
    public String vehicleBrand;
    public void getInfo(){
        System.out.printf("name:%d id:%d% brand:%d price:%f\n",getItemId(),itemname,vehicleBrand,getStartPrice());
    }
    public Vehicle(String itemName, String itemId,Double startPrice, String vehicleBrand){
        super(itemName, itemId, startPrice);
        this.vehicleBrand = vehicleBrand;
    }
    public String getVehicleBrand() {
        return vehicleBrand;
    }
    public void setVehicleBrand(String vehicleBrand) {
        this.vehicleBrand = vehicleBrand;
    }
    @Override
    public String toString() {
        return "itemId: " + getItemId() + ", itemName: " + getItemId() + ", currentPrice: " + getCurrentPrice() + ", brand:" + getVehicleBrand();
    }
}
