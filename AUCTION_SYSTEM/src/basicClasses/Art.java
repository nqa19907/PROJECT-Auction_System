package basicClasses;

public class Art extends Item {
    String author;
    String electronicBrand;
    public Art(String itemName, String itemId,String author, Double startPrice){
        super(itemName, itemId, startPrice);
        this.author = author;
    }
    @Override
    public void getInfo(){
        System.out.printf("name:%d id:%d% author:%d price:%f\n",getItemId(),itemName,author,getStartPrice());
    }

}
