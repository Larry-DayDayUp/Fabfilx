/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs three steps:
 *      1. Get parameter from request URL so it know which id to look for
 *      2. Use jQuery to talk to backend API to get the json data.
 *      3. Populate the data to correct html elements.
 */


/**
 * Retrieve parameter from request URL, matching by parameter name
 * @param target String
 * @returns {*}
 */
function getParameterByName(target) {
    // Get request URL
    let url = window.location.href;
    // Encode target parameter name to url encoding
    target = target.replace(/[\[\]]/g, "\\$&");

    // Ues regular expression to find matched parameter value
    let regex = new RegExp("[?&]" + target + "(=([^&#]*)|&|#|$)"),
        results = regex.exec(url);
    if (!results) return null;
    if (!results[2]) return '';

    // Return the decoded parameter value
    return decodeURIComponent(results[2].replace(/\+/g, " "));
}

/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */

function handleResult(resultData) {
    console.log("handleResult: populating movie info from resultData");

    const movie = resultData[resultData.length - 1];  // Get the first movie object
    let movieInfoElement = jQuery("#movie_info");

    // Create clickable links for each genre
    const genres = movie["genres"].split(", ").sort();  // Split genres into an array
    const genreLinks = genres.map(genre => {
        return `<a href="genre.html?genre=${encodeURIComponent(genre)}">${genre}</a>`;  // Create a link for each genre
    }).join(", ");  // Join the links into a single string

    // Append movie information with clickable genre links
    movieInfoElement.append(
        `<p>Movie Title: ${movie["movie_title"]}</p>` +
        `<p>Year: ${movie["movie_year"]}</p>` +
        `<p>Director: ${movie["movie_director"]}</p>` +
        `<p>Rating: ${movie["rating"]}</p>` +
        `<p>Genres: ${genreLinks}</p>`  // Add genre links
    );

    console.log("handleResult: populating movie table from resultData");

    let starTableBodyElement = jQuery("#star_table_body");
    // Store unique stars
    let uniqueStars = new Set();

    // Populate star information in the table
    for (let i = 0; i < resultData.length; i++) {
        if (!uniqueStars.has(resultData[i]["star_name"])) {
            uniqueStars.add(resultData[i]["star_name"]);

            let rowHTML = `<tr>
                <td><a href="single-star.html?id=${encodeURIComponent(resultData[i]['star_id'])}">${resultData[i]["star_name"]}</a></td>
                <td>${resultData[i]["star_dob"]}</td>
            </tr>`;

            // Append to the table body
            starTableBodyElement.append(rowHTML);
        }
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser\
 */

// Get id from URL
let movieId = getParameterByName('id');
console.log("Requesting data for movie ID:", movieId);


// Makes the HTTP GET request and registers on success callback function handleResult
jQuery.ajax({
    dataType: "json",  // Setting return data type
    method: "GET",// Setting request method
    url: "api/single-movie?id=" + movieId, // Setting request url, which is mapped by StarsServlet in Stars.java
    success: (resultData) => handleResult(resultData) // Setting callback function to handle data returned successfully by the SingleStarServlet
});