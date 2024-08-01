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

@WebServlet(name = "SearchBarServlet", urlPatterns = "/api/searchbar")
public class SearchBarServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
            System.err.println("Error initializing DataSource: " + e.getMessage());
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        String queryParam = request.getParameter("query");
        queryParam = (queryParam == null || queryParam.isBlank()) ? "%" : "%" + queryParam + "%";

        // Get results per page and current page from request parameters
        int resultsPerPage = Integer.parseInt(request.getParameter("resultsPerPage"));
        int currentPage = Integer.parseInt(request.getParameter("currentPage"));

        // Calculate offset for pagination
        int offset = (currentPage - 1) * resultsPerPage;

        try (Connection conn = dataSource.getConnection()) {
            // Updated SQL query with LIMIT and OFFSET
            String sqlQuery = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name SEPARATOR ', ') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, '|', s.name) SEPARATOR ', ') AS stars " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "WHERE m.title LIKE ? " +
                    "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                    "ORDER BY r.rating DESC, m.title ASC " +
                    "LIMIT ? OFFSET ?";

            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, queryParam);
            ps.setInt(2, resultsPerPage);  // Set limit for pagination
            ps.setInt(3, offset);  // Set offset for pagination

            ResultSet rs = ps.executeQuery();

            JsonArray jsonArray = new JsonArray();

            while (rs.next()) {
                JsonObject movieJson = new JsonObject();
                movieJson.addProperty("movie_id", rs.getString("id"));
                movieJson.addProperty("movie_title", rs.getString("title"));
                movieJson.addProperty("movie_year", rs.getInt("year"));
                movieJson.addProperty("movie_director", rs.getString("director"));
                movieJson.addProperty("rating", rs.getFloat("rating"));

                JsonArray genres = new JsonArray();
                String genresString = rs.getString("genres");
                if (genresString != null) {
                    for (String genre : genresString.split(", ")) {
                        genres.add(genre);
                    }
                }

                JsonArray stars = new JsonArray();
                String starsString = rs.getString("stars");
                if (starsString != null) {
                    for (String star : starsString.split(", ")) {
                        stars.add(star);
                    }
                }

                movieJson.add("genres", genres);
                movieJson.add("stars", stars);

                jsonArray.add(movieJson);
            }

            out.write(jsonArray.toString());  // Output results
            response.setStatus(200);  // Success
        } catch (SQLException e) {
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());
            response.setStatus(500);  // Internal Server Error
        } finally {
            out.close();  // Close the writer
        }
    }
}
