package models;

public class Electronic extends Item {
    String electronicBrand;
    public Electronic(String itemName, String itemId,String electronicBrand, Double startPrice){
        super(itemName, itemId, startPrice);
        this.electronicBrand = electronicBrand;
    }
    @Override
    public void getInfo(){
        System.out.printf("name:%d id:%d% brand:%d price:%f\n",getItemId(),itemName,electronicBrand,getStartPrice());
    }
}
