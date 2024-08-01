import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.jasypt.util.password.StrongPasswordEncryptor;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "EmployeeServlet", urlPatterns = "/api/employee_login")
public class EmployeeServlet extends HttpServlet {
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

        try (Connection conn = dataSource.getConnection()) {
            // Query to check if the employee exists and get the encrypted password
            String query = "SELECT password FROM employees WHERE email = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setString(1, email);

                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String storedPassword = rs.getString("password");
                        StrongPasswordEncryptor encryptor = new StrongPasswordEncryptor();

                        // Verify if the input password matches the encrypted password
                        if (encryptor.checkPassword(password, storedPassword)) {
                            // Successful login: create a session
                            HttpSession session = request.getSession();
                            session.setAttribute("employeeEmail", email);

                            responseJsonObject.addProperty("status", "success");
                            responseJsonObject.addProperty("message", "Login successful");
                        } else {
                            responseJsonObject.addProperty("status", "fail");
                            responseJsonObject.addProperty("message", "Incorrect password");
                        }
                    } else {
                        responseJsonObject.addProperty("status", "fail");
                        responseJsonObject.addProperty("message", "Employee with email " + email + " not found");
                    }
                }
            }
        } catch (SQLException e) {
            responseJsonObject.addProperty("status", "error");
            responseJsonObject.addProperty("message", "Database error");
            request.getServletContext().log("SQL error during employee login", e);
        }

        response.getWriter().write(responseJsonObject.toString());
    }
}
