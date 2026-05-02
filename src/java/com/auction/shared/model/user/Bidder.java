package src.java.com.auction.shared.model.user;
public class Bidder extends User {
    String name;

    public Bidder(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}