import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@WebServlet(name = "PlaceOrderServlet", urlPatterns = "/api/place-order")
public class PlaceOrderServlet extends HttpServlet {
    private static final Gson gson = new Gson();
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Unable to initialize datasource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Retrieve credit card and customer information from the request
        String firstName = request.getParameter("first_name");
        String lastName = request.getParameter("last_name");
        String cardNumber = request.getParameter("card_number");
        String expirationDate = request.getParameter("expiration_date");

        // Check for missing required fields
        if (firstName == null || lastName == null || cardNumber == null || expirationDate == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("Missing required fields."));
            return;
        }

        // Validate credit card information
        if (!validateCreditCard(firstName, lastName, cardNumber, expirationDate)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(gson.toJson("Invalid credit card information."));
            return;
        }

        // Record the sale
        recordSale(request);

        // Return success response
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("status", "success");
        successResponse.addProperty("message", "Order placed successfully.");
        response.getWriter().write(gson.toJson(successResponse));
    }

    private boolean validateCreditCard(String firstName, String lastName, String cardNumber, String expirationDate) {
        try (Connection conn = dataSource.getConnection()) {
            String sql = "SELECT * FROM creditcards WHERE firstName = ? AND lastName = ? AND id = ? AND expiration = ?";
            PreparedStatement pstmt = conn.prepareStatement(sql);
            pstmt.setString(1, firstName);
            pstmt.setString(2, lastName);
            pstmt.setString(3, cardNumber);
            pstmt.setString(4, expirationDate);

            ResultSet rs = pstmt.executeQuery();
            return rs.next(); // True if a matching record is found
        } catch (SQLException e) {
            e.printStackTrace(); // Log the error
            return false; // If an error occurs, consider validation failed
        }
    }

    private void recordSale(HttpServletRequest request) {
        try (Connection conn = dataSource.getConnection()) {
            HttpSession session = request.getSession();
            Integer customerId = (Integer) session.getAttribute("customerId");

            if (customerId == null) {
                throw new RuntimeException("Customer ID not found in session.");
            }

            String cartJson = (String) session.getAttribute("shoppingCart");
            if (cartJson == null) {
                throw new RuntimeException("Shopping cart not found in session.");
            }

            List<Map<String, Object>> shoppingCart = gson.fromJson(cartJson, new TypeToken<List<Map<String, Object>>>() {}.getType());

            String sql = "INSERT INTO sales (customerId, movieId, quantity, saleDate) VALUES (?, ?, ?, NOW())";
            PreparedStatement pstmt = conn.prepareStatement(sql);

            for (Map<String, Object> item : shoppingCart) {
                pstmt.setInt(1, customerId);
                pstmt.setString(2, (String) item.get("movie_id"));
                pstmt.setInt(3, (Integer) item.get("quantity"));

                pstmt.addBatch(); // Use batch insert to improve performance
            }

            pstmt.executeBatch(); // Execute the batch insert
        } catch (SQLException e) {
            e.printStackTrace(); // Handle SQL exceptions
        } catch (RuntimeException e) {
            e.printStackTrace(); // Handle custom exceptions
        }
    }
}