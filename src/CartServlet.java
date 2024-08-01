// CartServlet to handle adding/removing movies from the cart
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.servlet.annotation.*;
import com.google.gson.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.*;

@WebServlet(name = "CartServlet", urlPatterns = "/api/cart")
public class CartServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private static final DecimalFormat decimalFormat = new DecimalFormat("#.00");

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        List<CartItem> cart = getCartFromSession(session);

        String movieId = request.getParameter("movie_id");
        String movieTitle = request.getParameter("movie_title");

        CartItem existingItem = findCartItem(cart, movieId);
        CartItem cartItem;

        if (existingItem != null) {
            // If the movie exists, increment the quantity
            existingItem.setQuantity(existingItem.getQuantity() + 1);
            cartItem = existingItem;
        } else {
            // Otherwise, generate a random price and add a new item to the cart
            double randomPrice = Math.random() * 10; // Random price between $0 and $10
            cartItem = new CartItem(movieId, movieTitle, randomPrice);
            cart.add(cartItem);
        }

        session.setAttribute("cart", cart); // Update the cart in the session

        // Create a JSON response with the updated cart item details
        JsonObject cartItemJson = new JsonObject();
        cartItemJson.addProperty("movie_id", cartItem.getMovieId());
        cartItemJson.addProperty("movie_title", cartItem.getMovieTitle());
        cartItemJson.addProperty("quantity", cartItem.getQuantity());
        cartItemJson.addProperty("price", decimalFormat.format(cartItem.getPrice()));

        // Return the JSON response
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(gson.toJson(cartItemJson));
    }

    private List<CartItem> getCartFromSession(HttpSession session) {
        List<CartItem> cart = (List<CartItem>) session.getAttribute("cart");
        if (cart == null) {
            cart = new ArrayList<>();
        }
        return cart;
    }

    private CartItem findCartItem(List<CartItem> cart, String movieId) {
        for (CartItem item : cart) {
            if (item.getMovieId().equals(movieId)) {
                return item;
            }
        }
        return null;
    }
}