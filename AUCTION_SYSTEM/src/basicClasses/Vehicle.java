package basicClasses;
public class Vehicle extends Item {
    public String vehicleBrand;
    public void getInfo(){
        System.out.printf("name:%d id:%d% brand:%d price:%f\n",getItemId(),itemName,vehicleBrand,getStartPrice());
    }
    public Vehicle(String itemName, String itemId,String vehicleBrand, Double startPrice){
        super(itemName, itemId, startPrice);
        this.vehicleBrand = vehicleBrand;
    }
}
