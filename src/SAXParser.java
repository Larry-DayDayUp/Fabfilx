import java.io.IOException;
import java.sql.*;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class SAXParser extends DefaultHandler {

    private List<Actor> actors;
    private List<Cast> casts;
    private List<Movie> mainMovies;
    private List<Movie> castMovies;
    private List<Genre> genres;
    private String tempVal;
    private Actor tempActor;
    private Cast tempCast;
    private Movie tempMainMovie;
    private Movie tempCastMovie;
    private Genre tempGenre;
    private StringBuilder tempGenres;

    public SAXParser() {
        actors = new ArrayList<>();
        casts = new ArrayList<>();
        mainMovies = new ArrayList<>();
        castMovies = new ArrayList<>();
        genres = new ArrayList<>();
    }

    public void runExample() {
        parseDocument("actors63.xml");
        parseDocument("mains243.xml");
        parseDocument("casts124.xml");
        insertDataIntoDatabase();
    }

    private void insertDataIntoDatabase() {
        insertActorsIntoDatabase();
        //
        insertMoviesIntoDatabase();
        //
        insertGenresIntoDatabase();
        insertGenresInMoviesIntoDatabase();
        insertStarsInMoviesIntoDatabase();
    }


    private void insertActorsIntoDatabase() {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        int successfulInsertions = 0;
        int unsuccessfulInsertions = 0;

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            String query = "INSERT INTO stars (id, name, birthYear) VALUES (?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);

            Set<String> processedActors = new HashSet<>();

            for (Actor actor : actors) {
                if (isValidActorData(actor)) {
                    String stagename = actor.getStagename();
                    if (!processedActors.contains(stagename)) {
                        ps.setString(1, generateNewStarId(connection));
                        ps.setString(2, stagename);
                        if (actor.getDob() != null) {
                            ps.setInt(3, Integer.parseInt(actor.getDob()));
                        } else {
                            ps.setNull(3, Types.INTEGER);
                        }
                        ps.addBatch();
                        ps.executeBatch();
                        //ps.executeUpdate();
                        processedActors.add(stagename);
                        successfulInsertions++;
                    } else {
                        // Log duplicate actor
                        //System.out.println("Duplicate actor found: " + actor.getStagename() + ", DOB: " + actor.getDob());
                        unsuccessfulInsertions++;
                    }
                } else {
                    // Log inconsistent or unexpected data
                    //System.out.println("Inconsistent data found for actor: " + actor.getStagename() + ", DOB: " + actor.getDob());
                    unsuccessfulInsertions++;
                }
            }
            //System.out.println("Processed Actor"+ processedActors.size());
            System.out.println("Actors data inserted successfully.");
            System.out.println("Successful insertions: " + successfulInsertions);
            System.out.println("Unsuccessful insertions: " + unsuccessfulInsertions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidActorData(Actor actor) {
        // Check if actor data is valid
        String dob = actor.getDob();
        return actor.getStagename() != null && !actor.getStagename().isEmpty() &&
                (dob == null || dob.isEmpty() || dob.matches("^\\d{4}$"));

    }

    private String generateNewStarId(Connection connection) throws SQLException {
        String query = "SELECT MAX(id) AS maxId FROM stars";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        if (rs.next()) {
            String maxId = rs.getString("maxId");
            if (maxId != null) {
                // Assuming the ID format is a letter followed by numbers (e.g., "nm0000001")
                String prefix = maxId.substring(0, 2); // "nm"
                int num = Integer.parseInt(maxId.substring(2)) + 1;
                return prefix + String.format("%07d", num); // Increment and format with leading zeros
            }
        }
        return "nm0000001"; // Default first ID if no records found
    }

    private void insertMoviesIntoDatabase() {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        int successfulInsertions = 0;
        int unsuccessfulInsertions = 0;

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            String query = "INSERT INTO movies (id, title, year, director) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);

            Set<String> processedMovies = new HashSet<>();

            for (Movie movie : mainMovies) {
                // Validate movie data before inserting
                if (isValidMovieData(movie)) {
                    String movieId = movie.getId();
                    String director = movie.getDirector();
                    if (director != null && !director.isEmpty() && !processedMovies.contains(movieId)) {
                        ps.setString(1, movieId);
                        ps.setString(2, movie.getTitle());
                        ps.setInt(3, Integer.parseInt(movie.getYear()));
                        ps.setString(4, director);
                        ps.addBatch();
                        ps.executeBatch();
                        //ps.executeUpdate();
                        processedMovies.add(movieId); // Add movie ID to processed set
                        successfulInsertions++; // Increment successful insertion count
                    } else {
                        // Log invalid movie
                        //System.out.println("Invalid movie found: " + movieId + " " + movie.getTitle() + " " + movie.getYear() + " " + director);
                        unsuccessfulInsertions++; // Increment unsuccessful insertion count
                    }
                } else {
                    // Log inconsistent or unexpected data
                    //System.out.println("Inconsistent data found for movie: " + movie.getId() + " " + movie.getTitle() + " " + movie.getYear() + " " + movie.getDirector());
                    unsuccessfulInsertions++; // Increment unsuccessful insertion count
                }
            }
            //ps.executeBatch();
            //System.out.println("Processed Movie"+ processedMovies.size());
            System.out.println("Movies data inserted successfully.");
            System.out.println("Successful insertions: " + successfulInsertions);
            System.out.println("Unsuccessful insertions: " + unsuccessfulInsertions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private boolean isValidMovieData(Movie movie) {
        return movie.getId() != null && !movie.getId().isEmpty() &&
                movie.getTitle() != null && !movie.getTitle().isEmpty() &&
                movie.getYear().matches("^\\d{4}$");
    }


    private void insertGenresIntoDatabase() {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        int successfulInsertions = 0;
        int unsuccessfulInsertions = 0;

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            String query = "INSERT INTO genres (id, name) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(query);

            Set<String> processedGenres = new HashSet<>();

            for (Genre genre : genres) {
                String genreId = generateGenreId(connection); // Generate unique ID for the genre

                // Check if the genre name is not already processed
                if (!processedGenres.contains(genre.getGenreName())) {
                    ps.setString(1, genreId);
                    ps.setString(2, genre.getGenreName());
                    ps.executeUpdate();

                    System.out.println("Inserted genre: " + genre.getGenreName());
                    successfulInsertions++; // Increment successful insertion count

                    processedGenres.add(genre.getGenreName());
                } else {
                    // Log duplicate genre
                    //System.out.println("Duplicate genre found: " + genre.getGenreName());
                    unsuccessfulInsertions++; // Increment unsuccessful insertion count
                }
            }
            //System.out.println("Processed Genres"+processedGenres.size());
            System.out.println("Genres data inserted successfully.");
            System.out.println("Successful insertions: " + successfulInsertions);
            System.out.println("Unsuccessful insertions: " + unsuccessfulInsertions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String generateGenreId(Connection connection) throws SQLException {
        // Generate a unique genre ID based on the current maximum ID in the database
        String query = "SELECT MAX(id) AS maxId FROM genres";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(query);
        if (rs.next()) {
            int maxId = rs.getInt("maxId");
            return Integer.toString(maxId + 1); // Increment the maximum ID by 1
        }
        return "1"; // Default ID if no records found
    }


    private void insertGenresInMoviesIntoDatabase() {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        int successfulInsertions = 0;
        int unsuccessfulInsertions = 0;

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            String insertQuery = "INSERT INTO genres_in_movies (genreId, movieId) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertQuery);

            for (Movie movie : mainMovies) {
                String cat = movie.getCat();
                if (cat != null && !cat.trim().isEmpty()) {
                    String[] movieGenres = cat.split(",");
                    for (String genreName : movieGenres) {
                        genreName = genreName.trim();
                        if (!genreName.isEmpty()) {
                            String genreId = findGenreIdByName(connection, genreName);
                            if (genreId != null) {
                                ps.setString(1, genreId);
                                ps.setString(2, movie.getId());
                                ps.addBatch();

                                //System.out.println(successfulInsertions);
                                successfulInsertions++;
                            } else {
                                // Log error if genre ID not found
                                //System.out.println("Genre ID not found for genre: " + genreName);
                                unsuccessfulInsertions++;
                            }
                        }
                    }
                }
            }
            try {
                ps.executeBatch();
            } catch (BatchUpdateException bue) {
                System.out.println("Batch update exception: " + bue.getMessage());
                int[] updateCounts = bue.getUpdateCounts();
                for (int i = 0; i < updateCounts.length; i++) {
                    if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                        unsuccessfulInsertions++;
                    } else {
                        successfulInsertions++;
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL exception during batch execution: " + e.getMessage());
            }
            System.out.println("Genres in movies data inserted successfully.");
            System.out.println("Successful insertions: " + successfulInsertions);
            System.out.println("Unsuccessful insertions: " + unsuccessfulInsertions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String findGenreIdByName(Connection connection, String genreName) throws SQLException {
        String query = "SELECT id FROM genres WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, genreName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        }
        return null;
    }

    private void insertStarsInMoviesIntoDatabase() {
        String loginUser = "mytestuser";
        String loginPasswd = "My6$Password";
        String loginUrl = "jdbc:mysql://localhost:3306/moviedb";

        int successfulInsertions = 0;
        int unsuccessfulInsertions = 0;
        Map<String, String> starIdCache = new HashMap<>();

        try (Connection connection = DriverManager.getConnection(loginUrl, loginUser, loginPasswd)) {
            String insertQuery = "INSERT INTO stars_in_movies (starId, movieId) VALUES (?, ?)";
            PreparedStatement ps = connection.prepareStatement(insertQuery);

            for (Cast cast : casts) {
                if (cast.getMovieTitle() != null) {
                    String starId = starIdCache.computeIfAbsent(cast.getActor(), actorName -> {
                        try {
                            return findStarIdByName(connection, actorName);
                        } catch (SQLException e) {
                            e.printStackTrace();
                            return null;
                        }
                    });

                    if (starId != null) {
                        ps.setString(1, starId);
                        ps.setString(2, cast.getFilmId());
                        ps.addBatch();
                        System.out.println(successfulInsertions);
                        successfulInsertions++;
                    } else {
                        unsuccessfulInsertions++;
                    }
                }
            }

            try {
                ps.executeBatch();
            } catch (BatchUpdateException bue) {
                System.out.println("Batch update exception: " + bue.getMessage());
                int[] updateCounts = bue.getUpdateCounts();
                for (int i = 0; i < updateCounts.length; i++) {
                    if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                        // Log failed insertion for the corresponding record
                        System.out.println("Failed insertion for record: " + casts.get(i).getFilmId());
                        unsuccessfulInsertions++;
                    }
                }
            } catch (SQLException e) {
                System.out.println("SQL exception during batch execution: " + e.getMessage());
            }

            System.out.println("Stars in movies data inserted successfully.");
            System.out.println("Successful insertions: " + successfulInsertions);
            System.out.println("Unsuccessful insertions: " + unsuccessfulInsertions);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String findStarIdByName(Connection connection, String name) throws SQLException {
        String query = "SELECT id FROM stars WHERE name = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, name);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("id");
                }
            }
        }
        return null;
    }

    private void parseDocument(String fileName) {
        SAXParserFactory spf = SAXParserFactory.newInstance();
        try {
            javax.xml.parsers.SAXParser sp = spf.newSAXParser();
            sp.parse(fileName, this);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        tempVal = "";
        switch (qName.toLowerCase()) {
            case "actor":
                tempActor = new Actor();
                break;
            case "m":
                tempCast = new Cast();
                break;
            case "film":
                tempMainMovie = new Movie();
                tempGenres = new StringBuilder();
                break;
            case "filmc":
                tempCastMovie = new Movie();
                break;
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        tempVal = new String(ch, start, length).trim();
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        switch (qName.toLowerCase()) {
            case "actor":
                actors.add(tempActor);
                break;
            case "m":
                casts.add(tempCast);
                break;
            case "film":
                mainMovies.add(tempMainMovie);
                tempMainMovie.setCat(tempGenres.toString().trim()); // Set genres to cat field
                extractGenres(tempGenres.toString()); // Extract and add genres to the genres list
                tempMainMovie = null; // Reset temporary movie object
                tempGenres = new StringBuilder(); // Reset temporary genres string builder
                break;
            case "filmc":
                castMovies.add(tempCastMovie);
                break;
            case "stagename":
                tempActor.setStagename(tempVal);
                break;
            case "dob":
                tempActor.setDob(tempVal);
                break;
            case "f":
                tempCast.setFilmId(tempVal);
                break;
            case "a":
                tempCast.setActor(tempVal);
                break;
            case "fid":
                if (tempMainMovie != null) {
                    tempMainMovie.setId(tempVal);
                } else if (tempCastMovie != null) {
                    tempCastMovie.setId(tempVal);
                }
                break;
            case "t":
                if (tempMainMovie != null) {
                    tempMainMovie.setTitle(tempVal);
                } else if (tempCastMovie != null) {
                    tempCastMovie.setTitle(tempVal);
                }
                if (tempCast != null) {
                    tempCast.setMovieTitle(tempVal); // Set movie title for cast
                }
                break;
            case "year":
                if (tempMainMovie != null) {
                    tempMainMovie.setYear(tempVal);
                } else if (tempCastMovie != null) {
                    tempCastMovie.setYear(tempVal);
                }
                break;
            case "dirn":
                if (tempMainMovie != null) {
                    tempMainMovie.setDirector(tempVal);
                } // No director for cast movies
                break;
            case "cat":
                tempGenres.append(tempVal).append(", "); // Append genre to temporary genres string builder
                break;
        }
    }


    private void extractGenres(String genreString) {
        String[] genreArray = genreString.split(",");
        for (String genre : genreArray) {
            if (!genre.isEmpty()) {
                genres.add(new Genre(genre.trim())); // Add Genre objects to the genres list
            }
        }
    }


    public static void main(String[] args) {
        SAXParser spe = new SAXParser();
        spe.runExample();
    }
}

// Actor class
class Actor {
    private String stagename;
    private String dob;

    public String getStagename() {
        return stagename;
    }

    public void setStagename(String stagename) {
        this.stagename = stagename;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        // If dob is empty, set it to null
        if (dob != null && dob.isEmpty()) {
            this.dob = null;
        } else {
            // Set dob to the provided value
            this.dob = dob;
        }
    }


    private boolean isValidYear(String year) {
        try {
            int y = Integer.parseInt(year);
            return y > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}

// Cast class
class Cast {
    private String filmId;
    private String actor;
    private String movieTitle;

    public String getFilmId() {
        return filmId;
    }

    public void setFilmId(String filmId) {
        this.filmId = filmId;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }
}


class Movie {
    private String id;
    private String title;
    private String year;
    private String director;
    private String cat; // String to store genres

    public Movie() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getDirector() {
        return director;
    }

    public void setDirector(String director) {
        if (director != null && director.isEmpty()) {
            this.director = null;
        } else {
            this.director = director;
        }
    }

    public String getCat() {
        return cat;
    }

    public void setCat(String cat) {
        this.cat = cat;
    }
}


class Genre {
    private String genreName;
    private String genreId;
    public Genre(String genreName) {
        this.genreName = genreName;
    }

    public String getGenreName() {
        return genreName;
    }

    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }
    public String getGenreId() {
        return genreId;
    }

    public void setGenreId(String id) {
        this.genreId = id;
    }
}



//All the inconsistency report would be shown as prompt below
//The number you see is the part that we are doing insertion in mysql.
// I prefer to do it in local machine and use mysqldump to upload on AWS machine
// The reason I do it because my local machine have a faster CPU than my AWS machine
//Let me guide you to look at my prepared statement. I used them in all of my Servelt
//Let's wait until 33350. "33350" shows that there should be 33350 data being correctly inserted into database.
//After we finish inserting on local database, I will use mysqldump and upload it to AWS machine