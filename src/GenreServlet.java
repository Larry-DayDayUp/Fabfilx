import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;

@WebServlet(name = "GenreServlet", urlPatterns = "/api/genre")
public class GenreServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;
    private DataSource dataSource;

    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            throw new ServletException("Could not locate DataSource", e);
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        PrintWriter out = response.getWriter();

        // Get the genre from query parameters
        String genreName = request.getParameter("content");
        genreName = (genreName == null || genreName.isBlank()) ? "%" : "%" + genreName + "%";

        // Get results per page and current page from request parameters
        int resultsPerPage = Integer.parseInt(request.getParameter("resultsPerPage"));
        int currentPage = Integer.parseInt(request.getParameter("currentPage"));

        // Calculate offset for pagination
        int offset = (currentPage - 1) * resultsPerPage;
        int fetchLimit = resultsPerPage + 1;

        try (Connection conn = dataSource.getConnection()) {
            // SQL query to fetch movies of a specific genre
            String sqlQuery = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "GROUP_CONCAT(DISTINCT g.name SEPARATOR ', ') AS genres, " +
                    "GROUP_CONCAT(DISTINCT CONCAT(s.id, '|', s.name) SEPARATOR ', ') AS stars " +
                    "FROM movies m " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "WHERE g.name LIKE ? " +
                    "GROUP BY m.id, m.title, m.year, m.director, r.rating " +
                    "ORDER BY r.rating DESC, m.title ASC " +
                    "LIMIT ? OFFSET ?";

            PreparedStatement ps = conn.prepareStatement(sqlQuery);
            ps.setString(1, "%" + genreName + "%");
            ps.setInt(2, fetchLimit);
            ps.setInt(3, offset);  // Set offset for pagination

            ResultSet rs = ps.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // Iterate through the ResultSet and add each movie to the JSON array
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
                movieJson.add("genres", genres);

                JsonArray stars = new JsonArray();
                String starsString = rs.getString("stars");
                if (starsString != null) {
                    for (String star : starsString.split(", ")) {
                        stars.add(star);
                    }
                }
                movieJson.add("stars", stars);
                jsonArray.add(movieJson);
            }

            out.write(jsonArray.toString());
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {
            // Handle SQL exceptions
            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());

            // Return HTTP 500 status for server error
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close(); // Ensure PrintWriter is closed
        }
    }
}
