import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.io.IOException;
import java.sql.*;

@WebServlet(name = "OperationServlet", urlPatterns = "/api/operation")
public class OperationServlet extends HttpServlet {
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
        String action = request.getParameter("action");

        if ("insert_star".equals(action)) {
            insertStar(request, response);
        } else if ("add_movie".equals(action)) {
            addMovie(request, response);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unknown action: " + action);
        }
    }

    private void insertStar(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String starName = request.getParameter("starName");
        String starBirthYearStr = request.getParameter("starBirthYear");
        Integer starBirthYear = null;

        if (starBirthYearStr != null && !starBirthYearStr.trim().isEmpty()) {
            try {
                starBirthYear = Integer.parseInt(starBirthYearStr);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid birth year.");
                return;
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            // Check if the star already exists
            String checkStarSql = "SELECT id FROM stars WHERE name = ?";
            try (PreparedStatement checkStarStmt = connection.prepareStatement(checkStarSql)) {
                checkStarStmt.setString(1, starName);
                ResultSet rs = checkStarStmt.executeQuery();

                if (rs.next()) {
                    // Star already exists, return the existing starID
                    String existingStarId = rs.getString("id");
                    response.getWriter().write("Star already exists with ID: " + existingStarId);
                    return;
                }
            }


            String newStarId = generateNewId(connection, "nm");

            // Insert the new star with the generated ID
            String insertStarSql = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            try (PreparedStatement insertStarStmt = connection.prepareStatement(insertStarSql)) {
                insertStarStmt.setString(1, newStarId);
                insertStarStmt.setString(2, starName);
                insertStarStmt.setObject(3, starBirthYear);
                insertStarStmt.executeUpdate();

                // Return the new star ID in the response
                response.getWriter().write("Star successfully inserted with ID: " + newStarId);
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error inserting star.");
        }
    }


    private String generateNewId(Connection connection, String prefix) throws SQLException {
        String newId;
        String getMaxIdQuery = "SELECT MAX(id) AS max_id FROM ";
        if ("".equals(prefix)) {
            getMaxIdQuery += "genres";
        } else if ("nm".equals(prefix)) {
            getMaxIdQuery += "stars WHERE id LIKE BINARY 'nm%'";
        } else if ("tt".equals(prefix)) {
            getMaxIdQuery += "movies WHERE id LIKE BINARY 'tt%'";
        } else {
            throw new IllegalArgumentException("Unsupported prefix: " + prefix);
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(getMaxIdQuery)) {
            rs.next();
            String maxId = rs.getString("max_id");
            if (maxId == null) {
                if ("".equals(prefix)) {
                    newId = "1"; // For genres, start from 1
                } else {
                    newId = prefix + "0000001"; // For stars and movies, start with prefix followed by 0000001
                }
            } else {
                // Extract numeric part of the ID and increment it
                int numericPart = Integer.parseInt(maxId.substring(prefix.length())) + 1;
                // Format the numeric part with leading zeroes as needed
                String formattedNumericPart = String.format("%07d", numericPart);
                newId = prefix + formattedNumericPart;
            }
        }
        return newId;
    }


    private void addMovie(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String movieTitle = request.getParameter("movieTitle");
        String releaseYearStr = request.getParameter("releaseYear");
        String director = request.getParameter("director");
        String starName = request.getParameter("starName");
        String genreName = request.getParameter("genreName");

        Integer releaseYear = null;
        if (releaseYearStr != null && !releaseYearStr.trim().isEmpty()) {
            try {
                releaseYear = Integer.parseInt(releaseYearStr);
            } catch (NumberFormatException e) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid release year.");
                return;
            }
        }

        try (Connection connection = dataSource.getConnection()) {
            // Check if the movie already exists
            String checkMovieSql = "SELECT id FROM movies WHERE title = ? AND year = ? AND director = ?";
            try (PreparedStatement checkMovieStmt = connection.prepareStatement(checkMovieSql)) {
                checkMovieStmt.setString(1, movieTitle);
                checkMovieStmt.setObject(2, releaseYear);
                checkMovieStmt.setString(3, director);

                ResultSet rs = checkMovieStmt.executeQuery();
                if (rs.next()) {
                    // Movie already exists, get the movie ID
                    String existingMovieId = rs.getString("id");

                    // Link the star to the existing movie
                    linkStarToMovie(connection, existingMovieId, starName);

                    response.getWriter().write("Movie already exists with ID: " + existingMovieId);
                    return;
                }
            }

            // Movie does not exist, proceed with insertion
            String newMovieId = generateNewId(connection, "tt");

            // Insert the new movie with the generated ID
            String insertMovieSql = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertMovieStmt = connection.prepareStatement(insertMovieSql)) {
                insertMovieStmt.setString(1, newMovieId);
                insertMovieStmt.setString(2, movieTitle);
                insertMovieStmt.setObject(3, releaseYear);
                insertMovieStmt.setString(4, director);
                insertMovieStmt.executeUpdate();

                // Link star to the new movie
                linkStarToMovie(connection, newMovieId, starName);

                // Link genre to the new movie
                linkGenreToMovie(connection, newMovieId, genreName);

                // Return the new movie ID, genre ID, and star ID in the response
                response.getWriter().write("Movie successfully added with ID: " + newMovieId
                        + ", genre ID: " + getGenreId(connection, genreName) + ", star ID: " + getStarId(connection, starName));
            }
        } catch (SQLException e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error adding movie.");
        }
    }


    private String getGenreId(Connection connection, String genreName) throws SQLException {
        // Query the database to get the genreId for the given genreName
        String genreId = null;
        String getGenreIdSql = "SELECT id FROM genres WHERE name = ?";
        try (PreparedStatement getGenreIdStmt = connection.prepareStatement(getGenreIdSql)) {
            getGenreIdStmt.setString(1, genreName);
            ResultSet rs = getGenreIdStmt.executeQuery();
            if (rs.next()) {
                genreId = rs.getString("id");
            }
        }
        return genreId;
    }

    private String getStarId(Connection connection, String starName) throws SQLException {
        // Query the database to get the starId for the given starName
        String starId = null;
        String getStarIdSql = "SELECT id FROM stars WHERE name = ?";
        try (PreparedStatement getStarIdStmt = connection.prepareStatement(getStarIdSql)) {
            getStarIdStmt.setString(1, starName);
            ResultSet rs = getStarIdStmt.executeQuery();
            if (rs.next()) {
                starId = rs.getString("id");
            }
        }
        return starId;
    }


    private void linkStarToMovie(Connection connection, String movieId, String starName) throws SQLException {
        // Check if the star already exists
        String checkStarSql = "SELECT id FROM stars WHERE name = ?";
        String starId;

        try (PreparedStatement checkStarStmt = connection.prepareStatement(checkStarSql)) {
            checkStarStmt.setString(1, starName);
            ResultSet rs = checkStarStmt.executeQuery();

            if (rs.next()) {
                starId = rs.getString("id"); // Use existing star ID
            } else {
                // Create a new star
                String insertStarSql = "INSERT INTO stars (id, name) VALUES (?, ?)";
                try (PreparedStatement insertStarStmt = connection.prepareStatement(insertStarSql)) {
                    starId = generateNewId(connection, "nm");

                    insertStarStmt.setString(1, starId);
                    insertStarStmt.setString(2, starName);
                    insertStarStmt.executeUpdate();
                }
            }

            // Check if the star is already linked to the movie
            String checkLinkSql = "SELECT * FROM stars_in_movies WHERE starId = ? AND movieId = ?";
            try (PreparedStatement checkLinkStmt = connection.prepareStatement(checkLinkSql)) {
                checkLinkStmt.setString(1, starId);
                checkLinkStmt.setString(2, movieId);
                ResultSet linkRs = checkLinkStmt.executeQuery();


                if (!linkRs.next()) {
                    // Link star to movie
                    String linkStarSql = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
                    try (PreparedStatement linkStarStmt = connection.prepareStatement(linkStarSql)) {
                        linkStarStmt.setString(1, starId);
                        linkStarStmt.setString(2, movieId);
                        linkStarStmt.executeUpdate();
                    }
                }
            }
        }
    }

    private void linkGenreToMovie(Connection connection, String movieId, String genreName) throws SQLException {
        // Check if the genre already exists
        String checkGenreSql = "SELECT id FROM genres WHERE name = ?";
        String genreId;

        try (PreparedStatement checkGenreStmt = connection.prepareStatement(checkGenreSql)) {
            checkGenreStmt.setString(1, genreName);
            ResultSet rs = checkGenreStmt.executeQuery();

            if (rs.next()) {
                genreId = rs.getString("id"); // Use existing genre ID
            } else {
                // Create a new genre
                String insertGenreSql = "INSERT INTO genres (id, name) VALUES (?, ?)";
                try (PreparedStatement insertGenreStmt = connection.prepareStatement(insertGenreSql)) {
                    genreId = generateNewId(connection, "");

                    insertGenreStmt.setString(1, genreId);
                    insertGenreStmt.setString(2, genreName);
                    insertGenreStmt.executeUpdate();
                }
            }

            // Check if the genre is already linked to the movie
            String checkLinkSql = "SELECT * FROM genres_in_movies WHERE genreId = ? AND movieId = ?";
            try (PreparedStatement checkLinkStmt = connection.prepareStatement(checkLinkSql)) {
                checkLinkStmt.setString(1, genreId);
                checkLinkStmt.setString(2, movieId);
                ResultSet linkRs = checkLinkStmt.executeQuery();

                if (!linkRs.next()) {
                    // Link genre to movie
                    String linkGenreSql = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
                    try (PreparedStatement linkGenreStmt = connection.prepareStatement(linkGenreSql)) {
                        linkGenreStmt.setString(1, genreId);
                        linkGenreStmt.setString(2, movieId);
                        linkGenreStmt.executeUpdate();
                    }
                }
            }
        }
    }
}
