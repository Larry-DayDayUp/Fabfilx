import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        JsonArray jsonArray = new JsonArray();

        String query = request.getParameter("query");
        if (query == null || query.trim().isEmpty()) {
            response.getWriter().write(jsonArray.toString());
            return;
        }

        String[] keywords = query.trim().split("\\s+");
        StringBuilder sql = new StringBuilder("SELECT id, title FROM movies WHERE ");

        for (int i = 0; i < keywords.length; i++) {
            sql.append("title LIKE ?");
            if (i < keywords.length - 1) {
                sql.append(" AND ");
            }
        }

        // Limit to 10 rows and order by title in ascending order
        sql.append(" ORDER BY title ASC LIMIT 10");

        try (Connection conn = dataSource.getConnection()) {
            PreparedStatement statement = conn.prepareStatement(sql.toString());
            for (int i = 0; i < keywords.length; i++) {
                statement.setString(i + 1, "%" + keywords[i] + "%");
            }

            ResultSet rs = statement.executeQuery();

            while (rs.next()) {
                String movieId = rs.getString("id");
                String movieTitle = rs.getString("title");

                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("value", movieTitle);

                JsonObject additionalDataJsonObject = new JsonObject();
                additionalDataJsonObject.addProperty("movie_id", movieId);

                jsonObject.add("data", additionalDataJsonObject);
                jsonArray.add(jsonObject);
            }
            //response.setContentType("application/json");
            response.getWriter().write(jsonArray.toString());
        } catch (SQLException e) {
            e.printStackTrace();
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }
}
