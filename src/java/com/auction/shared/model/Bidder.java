public class Bidder extends userManager{
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