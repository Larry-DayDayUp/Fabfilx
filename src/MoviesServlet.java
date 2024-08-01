import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.LinkedHashMap;


// Declaring a WebServlet called MoviesServlet, which maps to url "/api/stars"
@WebServlet(name = "MoviesServlet", urlPatterns = "/api/movies")
public class MoviesServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/moviedb");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from dataSource and let resource manager close the connection after usage.
        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String subQuery = "SELECT m.id FROM movies m LEFT JOIN ratings r ON m.id = r.movieId ORDER BY r.rating DESC LIMIT 20";
            String query = "SELECT m.id, m.title, m.year, m.director, r.rating, " +
                    "g.name as genre_name, s.name as star_name, s.id AS star_id " +
                    "FROM (" + subQuery + ") AS top_movies " +
                    "JOIN movies m ON m.id = top_movies.id " +
                    "LEFT JOIN genres_in_movies gm ON m.id = gm.movieId " +
                    "LEFT JOIN genres g ON gm.genreId = g.id " +
                    "LEFT JOIN stars_in_movies sm ON m.id = sm.movieId " +
                    "LEFT JOIN stars s ON sm.starId = s.id " +
                    "LEFT JOIN ratings r ON m.id = r.movieId " +
                    "ORDER BY r.rating DESC, m.id, g.name, s.name ";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            LinkedHashMap<String, JsonObject> movies = new LinkedHashMap<>();
            // Iterating through each row of the ResultSet
            while (rs.next() && movies.size() < 20) {
                String movieId = rs.getString("id");
                JsonObject movieJson = movies.computeIfAbsent(movieId, k -> {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("movie_id", movieId);
                    try {
                        obj.addProperty("movie_title", rs.getString("title"));
                        obj.addProperty("movie_year", rs.getInt("year"));
                        obj.addProperty("movie_director", rs.getString("director"));
                        obj.addProperty("rating", rs.getFloat("rating"));
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                    obj.add("genres", new JsonArray());  // Initialize genres array
                    obj.add("stars", new JsonArray());   // Initialize stars array
                    return obj;
                });

                // Collect up to three genres
                JsonArray genres = movieJson.getAsJsonArray("genres");
                if (genres.size() < 3) {
                    String genreName = rs.getString("genre_name");
                    if (genreName != null && !genres.toString().contains(genreName)) {  // Check if genre is not already included
                        genres.add(genreName);
                    }
                }

                // Collect up to three stars
                JsonArray stars = movieJson.getAsJsonArray("stars");
                if (stars.size() < 3) {
                    String starId = rs.getString("star_id");  // Get star ID from result set
                    String starName = rs.getString("star_name");  // Get star name from result set
                    JsonObject starObject = new JsonObject();
                    starObject.addProperty("star_id", starId);
                    starObject.addProperty("star_name", starName);

                    // Ensure not to add duplicate star entries
                    boolean exists = false;
                    for (int i = 0; i < stars.size(); i++) {
                        JsonObject existingStar = stars.get(i).getAsJsonObject();
                        if (existingStar.get("star_id").getAsString().equals(starId)) {
                            exists = true;
                            break;
                        }
                    }

                    if (!exists) {
                        stars.add(starObject);
                    }
                }
            }

            movies.values().forEach(jsonArray::add);

            rs.close();
            out.write(jsonArray.toString());
            response.setStatus(200);

        } catch (Exception e) {

            // Write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }

        // Always remember to close db connection after usage. Here it's done by try-with-resources

    }
}