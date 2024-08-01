public class CartItem {
    private String movieId;
    private String movieTitle;
    private int quantity;
    private double price;

    // Constructor with default quantity of 1
    public CartItem(String movieId, String movieTitle, double price) {
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.quantity = 1; // Default quantity is 1
        this.price = price;
    }

    // Getters and Setters
    public String getMovieId() {
        return movieId;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public int getQuantity() {
        return quantity;
    }

    public double getPrice() {
        return price;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public void setPrice(double price) {
        this.price = price;
    }
}