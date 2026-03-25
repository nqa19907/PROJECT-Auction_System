package basicClasses;
public abstract class User extends Entity {
    private String username;
    private String password;
    private String email;
    private final String userId;
    public User(String a,String b, String c, String d){
        username = a;
        password = b;
        email = c;
        userId = d;
    }
    public String getId(){
        return userId;
    }
    public String getPassword(){
        return password;
    }
    public String getEmail(){
        return email;
    }
    public String getUsername(){
        return username;
    }


}