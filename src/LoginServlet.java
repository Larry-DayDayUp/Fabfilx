import com.google.gson.JsonObject;
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

@WebServlet(name = "LoginServlet", urlPatterns = "/api/login")
public class LoginServlet extends HttpServlet {
    private DataSource dataSource;

    @Override
    public void init() {
        try {
            InitialContext context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:/comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new RuntimeException("Unable to initialize DataSource", e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String email = request.getParameter("email");
        String password = request.getParameter("password");

        JsonObject responseJsonObject = new JsonObject();

        String query = "SELECT email, password FROM customers WHERE email = ?";

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, email);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String storedPassword = rs.getString("password");
                    if (storedPassword.equals(password)) {
                        // Login successful
                        HttpSession session = request.getSession();
                        session.setAttribute("user", email); // Store the email in the session

                        responseJsonObject.addProperty("status", "success");
                        responseJsonObject.addProperty("message", "Login successful");
                    } else {
                        // Incorrect password
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Incorrect password");
                    }
                } else {
                    // Email not found
                    responseJsonObject.addProperty("status", "fail");
                    responseJsonObject.addProperty("message", "User " + email + " doesn't exist");
                }
            }
        } catch (SQLException e) {
            // Handle SQL error
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", "Database error");
            request.getServletContext().log("Database error during login", e);
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}