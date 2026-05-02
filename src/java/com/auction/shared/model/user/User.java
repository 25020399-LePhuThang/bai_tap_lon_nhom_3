package src.java.com.auction.shared.model.user;

import org.w3c.dom.Entity;

public abstract class User implements Entity {
    private static final long serialVersionUID = 1L;
    protected String name;
    protected String password;
    protected String email;
    protected long phonenumber;
    protected String status;
    protected String id;
    public User(String id, String name,String password,String email,long phonenumber,String status) {
        this.name=name;
        this.password=password;
        this.email=email;
        this.phonenumber=phonenumber;
        this.status=status;
        this.id=id;
    }
    public User(){};

    public String getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setPhonenumber(long phonenumber) {
        this.phonenumber = phonenumber;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public long getPhonenumber() {
        return phonenumber;
    }

    public String getStatus() {
        return status;
    }
}