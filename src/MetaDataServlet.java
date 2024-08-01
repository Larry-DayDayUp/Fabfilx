import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;

@WebServlet(name = "MetaDataServlet", urlPatterns = "/api/metadata")
public class MetaDataServlet extends HttpServlet {
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        try (Connection connection = dataSource.getConnection()) {
            JsonArray tablesArray = new JsonArray();

            // Using a PreparedStatement to get the list of table names in the 'moviedb' schema
            String getAllTablesQuery = "SELECT TABLE_NAME FROM information_schema.TABLES WHERE TABLE_SCHEMA = ?";
            try (PreparedStatement statement = connection.prepareStatement(getAllTablesQuery)) {
                statement.setString(1, "moviedb"); // Set the schema name
                ResultSet resultSet = statement.executeQuery();

                while (resultSet.next()) {
                    String tableName = resultSet.getString("TABLE_NAME");
                    System.out.println("Retrieved table: " + tableName); // Log the retrieved table name

                    // Get the column metadata for each table
                    DatabaseMetaData metaData = connection.getMetaData();
                    ResultSet columns = metaData.getColumns(null, "moviedb", tableName, null);
                    JsonArray columnsArray = new JsonArray();

                    // Use a HashMap to store the columns for each table
                    HashMap<String, JsonObject> columnMap = new HashMap<>();

                    while (columns.next()) {
                        String columnName = columns.getString("COLUMN_NAME");
                        // Check if the column name already exists for the table
                        if (!columnMap.containsKey(columnName)) {
                            JsonObject columnObject = new JsonObject();
                            columnObject.addProperty("columnName", columnName);
                            columnObject.addProperty("dataType", columns.getString("TYPE_NAME"));
                            columnObject.addProperty("columnSize", columns.getInt("COLUMN_SIZE"));
                            columnObject.addProperty("nullable", columns.getString("IS_NULLABLE"));
                            columnsArray.add(columnObject); // Add column metadata to the array
                            // Store the column object in the map
                            columnMap.put(columnName, columnObject);
                        }
                    }

                    // Create JSON object for each table with column information
                    JsonObject tableObject = new JsonObject();
                    tableObject.addProperty("tableName", tableName);
                    tableObject.add("columns", columnsArray); // Add column metadata array

                    tablesArray.add(tableObject); // Add the table to the response array
                }

            }

            // Final response JSON with the metadata of all tables in 'moviedb'
            JsonObject responseJson = new JsonObject();
            responseJson.add("tables", tablesArray);
            response.getWriter().write(responseJson.toString());

        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error retrieving metadata.");
        }
    }
}