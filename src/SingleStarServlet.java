import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called SingleStarServlet, which maps to URL "/api/single-star"
@WebServlet(name = "SingleStarServlet", urlPatterns = "/api/single-star")
public class SingleStarServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

    // Create a DataSource registered in web.xml
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
        response.setContentType("application/json"); // Response MIME type

        // Retrieve parameter id from URL request
        String id = request.getParameter("id");

        // Log the incoming request
        request.getServletContext().log("getting id: " + id);

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        // Get a connection from DataSource and let resource manager close the connection after usage
        try (Connection conn = dataSource.getConnection()) {
            // Construct a query with sorting for movies by year and then by title
            String query = "SELECT s.id AS starId, s.name, s.birthYear, " +
                    "m.id AS movieId, m.title, m.year, m.director " +
                    "FROM stars s " +
                    "JOIN stars_in_movies sim ON s.id = sim.starId " +
                    "JOIN movies m ON sim.movieId = m.id " +
                    "WHERE s.id = ? " +
                    "ORDER BY m.year DESC, m.title ASC"; // Sort by year, then by title

            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, id); // Set the parameter represented by "?" in the query to the star's ID

            ResultSet rs = statement.executeQuery(); // Perform the query

            JsonArray jsonArray = new JsonArray(); // Initialize JSON array for storing results

            // Iterate through the result set
            while (rs.next()) {
                String starId = rs.getString("starId");
                String starName = rs.getString("name");
                String starDob = rs.getString("birthYear");

                String movieId = rs.getString("movieId");
                String movieTitle = rs.getString("title");
                String movieYear = rs.getString("year");
                String movieDirector = rs.getString("director");

                // Create a JsonObject based on the retrieved data
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("star_id", starId);
                jsonObject.addProperty("star_name", starName);
                jsonObject.addProperty("star_dob", starDob);
                jsonObject.addProperty("movie_id", movieId);
                jsonObject.addProperty("movie_title", movieTitle);
                jsonObject.addProperty("movie_year", movieYear);
                jsonObject.addProperty("movie_director", movieDirector);

                jsonArray.add(jsonObject); // Add JSON object to the array
            }

            rs.close();
            statement.close();

            // Write JSON array to output
            out.write(jsonArray.toString());
            response.setStatus(200); // Set response status to 200 (OK)

        } catch (Exception e) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // Log error to localhost log
            request.getServletContext().log("Error:", e);
            response.setStatus(500); // Set response status to 500 (Internal Server Error)

        } finally {
            out.close(); // Close the output stream
        }
    }
}