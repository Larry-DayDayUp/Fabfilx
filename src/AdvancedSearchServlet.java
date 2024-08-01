import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "AdvancedSearchServlet", urlPatterns = "/api/advancedsearch")
public class AdvancedSearchServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config){
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
            System.out.println("DataSource initialized successfully.");
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Error initializing DataSource: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Extract search parameters
        String titleParam = request.getParameter("title");
        String yearParam = request.getParameter("year");
        String directorParam = request.getParameter("director");
        String starParam = request.getParameter("star");

        // Set parameters with default wildcards if empty
        titleParam = (titleParam == null) ? "" : titleParam;
        yearParam = (yearParam == null) ? "" : yearParam;
        directorParam = (directorParam == null) ? "" : directorParam;
        starParam = (starParam == null) ? "" : starParam;

        // Initialize conditions list
        List<String> conditions = new ArrayList<>();

        // Add conditions for each field if not empty
        if (!titleParam.isBlank()) {
            conditions.add("m.title LIKE ?");
        }
        if (!yearParam.isBlank()) {
            conditions.add("m.year = ?");
        }
        if (!directorParam.isBlank()) {
            conditions.add("m.director LIKE ?");
        }
        if (!starParam.isBlank()) {
            conditions.add("s.name LIKE ?");
        }

        try (Connection conn = dataSource.getConnection()) {
            // Corrected SQL query with proper syntax
            String sqlQuery = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name SEPARATOR ', ') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, '|', s.name) SEPARATOR ', ') AS stars " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id ";

            // Build the WHERE clause based on conditions
            String whereClause = "";
            if (!conditions.isEmpty()) {
                whereClause = "WHERE " + String.join(" AND ", conditions);
            }

            sqlQuery += whereClause; // Add the WHERE clause to the SQL query

            // Grouping and ordering clauses remain the same
            sqlQuery += " GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                    "ORDER BY r.rating DESC, m.id ";

            PreparedStatement ps = conn.prepareStatement(sqlQuery);

            // Set the parameters
            int parameterIndex = 1;
            if (!titleParam.isBlank()) {
                ps.setString(parameterIndex++, "%" + titleParam + "%");
            }
            if (!yearParam.isBlank()) {
                ps.setInt(parameterIndex++, Integer.parseInt(yearParam));
            }
            if (!directorParam.isBlank()) {
                ps.setString(parameterIndex++, "%" + directorParam + "%");
            }
            if (!starParam.isBlank()) {
                ps.setString(parameterIndex++, "%" + starParam + "%");
            }

            ResultSet rs = ps.executeQuery(); // Execute the query

            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("movie_id", rs.getString("id"));
                movieJson.addProperty("movie_title", rs.getString("title"));
                movieJson.addProperty("movie_year", rs.getInt("year"));
                movieJson.addProperty("movie_director", rs.getString("director"));
                movieJson.addProperty("rating", rs.getFloat("rating"));

                // Handling genres
                JsonArray genres = new JsonArray();
                String genresString = rs.getString("genres");
                if (genresString != null) {
                    for (String genre : genresString.split(", ")) {
                        genres.add(genre);
                    }
                }
                movieJson.add("genres", genres);

                JsonArray stars = new JsonArray();
                String starsString = rs.getString("stars");
                if (starsString != null) {
                    for (String star : starsString.split(", ")) {
                        stars.add(star);
                    }
                }
                movieJson.add("stars", stars);

                jsonArray.add(movieJson); // Add to the JSON array
            }

            out.write(jsonArray.toString()); // Write the JSON array to response
            response.setStatus(200); // Successful HTTP status
        } catch (SQLException e) {
            // Handle SQL errors
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500); // HTTP 500 for errors
        } finally {
            out.close(); // Close the output stream
        }
    }
}