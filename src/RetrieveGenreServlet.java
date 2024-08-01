import jakarta.servlet.*;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import com.google.gson.JsonObject;
import javax.sql.DataSource;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(name = "RetrieveGenreServlet", urlPatterns = "/api/retrieveGenres")
public class RetrieveGenreServlet extends HttpServlet {
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

        try (Connection conn = dataSource.getConnection()) {
            List<String> genres = retrieveGenres(conn);
            String json = convertToJson(genres);
            out.write(json);
            response.setStatus(HttpServletResponse.SC_OK);
        } catch (SQLException e) {

            JsonObject errorJson = new JsonObject();
            errorJson.addProperty("errorMessage", e.getMessage());
            out.write(errorJson.toString());

            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            out.close();
        }
    }

    private List<String> retrieveGenres(Connection connection) throws SQLException {
        List<String> genres = new ArrayList<>();
        String sql = "SELECT name FROM genres";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            ResultSet resultSet = statement.executeQuery();
            while (resultSet.next()) {
                genres.add(resultSet.getString("name"));
            }
        }
        return genres;
    }

    private String convertToJson(List<String> genres) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < genres.size(); i++) {
            json.append("\"").append(genres.get(i)).append("\"");
            if (i < genres.size() - 1) {
                json.append(",");
            }
        }
        json.append("]");
        return json.toString();
    }
}
