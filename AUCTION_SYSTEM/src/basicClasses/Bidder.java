package basicClasses;
import java.util.Scanner;
public class Bidder extends User {
    private double balance = 0; 
    Scanner sc = new Scanner(System.in);
    public Bidder(String a,String b, String c, String d){
        super(a, b, c, d);
    }
    public double getBalance(){
        return balance;
    }
    public void deposit(double a){
        if (a<0){
            System.out.println("so tien nhap phai lon hon 0\n");
            deposit( a = Double.parseDouble(sc.nextLine()));
        }
        else balance+=a;
    }
    
    
}
