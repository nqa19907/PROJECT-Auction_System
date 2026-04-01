package models;
import java.util.List;
import java.util.ArrayList;
public class Seller extends User {
    private double balance;
    private List<Item> items = new ArrayList<>();
    public Seller(String a,String b, String c, String d){
        super(a, b, c, d);
    }
    public void addItem(Item i){
        items.add(i);
    }
    public double getBalance(){
        return balance;
    }
    public void getItems(){
        for (Item i : items){
            System.out.printf("",i.itemName,i.getStartPrice());
        }
    }
}
