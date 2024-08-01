# CS122B Project 4

Submitted by: **Runlin Wang**

This web app: **project 4**

Time spent: **4 weeks** hours spent in total

## Required Features
Project4:
- Filenames with Prepared Statements: All the filename end with "Servlet"

## Contributions
Project 1
- RunLin Wang: Set up Task 1-5
- Emily Gao Wang: Task 1-3,6

Project2
- RunLin Wang: Main page (Search and Browse)
- Emily Gao Wang: Login page

Project3
- Runlin Wang: Task 1-6

Project4
- Runlin Wang: Task 1-4
- 
## Video Walkthrough

Project 1's walkthrough:
https://www.youtube.com/watch?v=0hMl1bKtbgQ

Project 2's walkthrough:
https://www.youtube.com/watch?v=qyWAd9CeBj4

Project 3's walkthrough:
https://www.youtube.com/watch?v=Yppd2qVg17Y

Project 4's walkthrough:
## Notes


- # Connection Pooling
    - #### [AdvancedSearchServlet](src/AdvancedSearchServlet.java) [AutocompleteServlet](src/AutocompleteServlet.java) [CartServlet](src/CartServlet.java) [EmployeeServlet](src/EmployeeServlet.java) [GenreServlet](src/GenreServlet.java) [LoginServlet](src/LoginServlet.java) [MetaDataServlet](src/MetaDataServlet.java) [MoviesServlet](src/MoviesServlet.java) [OperationServlet](src/OperationServlet.java) [PlaceOrderServlet](src/PlaceOrderServlet.java) [RetrieveGenreServlet](src/RetrieveGenreServlet.java) [SearchBarServlet](src/SearchBarServlet.java) [SingleMovie](src/SingleMovieServlet.java) [SingleStarServlet](src/SingleStarServlet.java)

    - #### Explain how Connection Pooling is utilized in the Fabflix code.
            Connection pooling in the Fabflix code is utilized through the configuration of a DataSource in the `context.xml` file and referenced in the `web.xml` file. This setup allows servlets to obtain a connection from the pool, managed by Tomcat, ensuring efficient reuse of database connections.

### Configuration Steps

1. **Define DataSource in `context.xml`**:
    ```xml
    <?xml version="1.0" encoding="UTF-8"?>
    <Context>
        <Resource name="jdbc/moviedb"
                  auth="Container"
                  type="javax.sql.DataSource"
                  maxTotal="100" maxIdle="30" maxWaitMillis="10000"
                  username="mytestuser"
                  password="My6$Password"
                  driverClassName="com.mysql.cj.jdbc.Driver"
                  url="jdbc:mysql://localhost:3306/moviedb?autoReconnect=true&amp;allowPublicKeyRetrieval=true&amp;useSSL=false"/>
    </Context>
    ```

2. **Resource Reference in `web.xml`**:
    ```xml
    <web-app>
        ...
        <resource-ref>
            <description>DB Connection</description>
            <res-ref-name>jdbc/moviedb</res-ref-name>
            <res-type>javax.sql.DataSource</res-type>
            <res-auth>Container</res-auth>
        </resource-ref>
        ...
    </web-app>
    ```

   3. **Usage in Java Code**:
       ```java
       import java.io.IOException;
       import java.sql.Connection;
       import java.sql.PreparedStatement;
       import java.sql.ResultSet;
       import java.util.ArrayList;
       import javax.annotation.Resource;
       import javax.servlet.ServletException;
       import javax.servlet.http.HttpServlet;
       import javax.servlet.http.HttpServletRequest;
       import javax.servlet.http.HttpServletResponse;
       import javax.sql.DataSource;

       public class AdvancedSearchServlet extends HttpServlet {
           private static final long serialVersionUID = 1L;

           @Resource(name = "jdbc/moviedb")
           private DataSource dataSource;

           protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
               try (Connection conn = dataSource.getConnection()) {
                   String searchQuery = "SELECT * FROM movies WHERE title LIKE ?";
                   try (PreparedStatement statement = conn.prepareStatement(searchQuery)) {
                       statement.setString(1, "%" + request.getParameter("title") + "%");
                       try (ResultSet rs = statement.executeQuery()) {
                           ArrayList<Movie> movies = new ArrayList<>();
                           while (rs.next()) {
                               Movie movie = new Movie();
                               movie.setId(rs.getInt("id"));
                               movie.setTitle(rs.getString("title"));
                               movie.setYear(rs.getInt("year"));
                               movies.add(movie);
                           }
                           request.setAttribute("movies", movies);
                           request.getRequestDispatcher("/WEB-INF/advancedSearchResults.jsp").forward(request, response);
                       }
                   }
               } catch (Exception e) {
                   e.printStackTrace();
                   response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An error occurred while processing your request.");
               }
           }
       }
      ```
       - #### Explain how Connection Pooling works with two backend SQL.
        When utilizing two backend SQL servers (a master and a slave), connection pooling helps manage the distribution of database connections efficiently. The setup typically involves:
  
        1. **Write Operations**:
        - All write operations are directed to the master SQL server to maintain data consistency.
  
        2. **Read Operations**:
        - Read operations can be load-balanced between the master and slave SQL servers to distribute the read load and improve performance.
  
        3. **DataSource Configuration**:
        - Define two DataSource configurations in the `context.xml` file, one for the master and one for the slave.
            ```xml
            <Context>
                <Resource name="jdbc/moviedb_master"
                          auth="Container"
                          type="javax.sql.DataSource"
                          maxTotal="100" maxIdle="30" maxWaitMillis="10000"
                          username="mytestuser"
                          password="My6$Password"
                          driverClassName="com.mysql.cj.jdbc.Driver"
                          url="jdbc:mysql://master_db_ip:3306/moviedb?autoReconnect=true&amp;allowPublicKeyRetrieval=true&amp;useSSL=false"/>
  
                <Resource name="jdbc/moviedb_slave"
                          auth="Container"
                          type="javax.sql.DataSource"
                          maxTotal="100" maxIdle="30" maxWaitMillis="10000"
                          username="mytestuser"
                          password="My6$Password"
                          driverClassName="com.mysql.cj.jdbc.Driver"
                          url="jdbc:mysql://slave_db_ip:3306/moviedb?autoReconnect=true&amp;allowPublicKeyRetrieval=true&amp;useSSL=false"/>
            </Context>
            ```
  
        4. **Application Logic**:
        - In the application code, determine the type of operation (read or write) and use the appropriate DataSource.
        - Example:
            ```java
            // For read operations
            @Resource(name = "jdbc/moviedb_slave")
            private DataSource slaveDataSource;
  
            // For write operations
            @Resource(name = "jdbc/moviedb_master")
            private DataSource masterDataSource;
  
            // Use slaveDataSource for read operations
            // Use masterDataSource for write operations
            ```
  
        5. **Load Balancer**:
        - The load balancer ensures that requests are evenly distributed between the Tomcat instances, each of which can connect to either the master or slave SQL server depending on the operation.

