/**
 * This example is following frontend and backend separation.
 *
 * Before this .js is loaded, the html skeleton is created.
 *
 * This .js performs two steps:
 *      1. Use jQuery to talk to backend API to get the json data.
 *      2. Populate the data to correct html elements.
 */


/**
 * Handles the data returned by the API, read the jsonObject and populate data into html elements
 * @param resultData jsonObject
 */
function handleMovieResult(resultData) {
    console.log("handleMovieResult: populating movie table from resultData");
    // Populate the star table
    // Find the empty table body by id "movie_table_body"
    let movieTableBodyElement = jQuery("#movie_table_body");

    console.log("Total results:", resultData.length); // Check how many movies are there

    for (let i = 0; i < resultData.length; i++) {
        let stars = "";
        if (resultData[i]["stars"] && Array.isArray(resultData[i]["stars"])) {
            stars = resultData[i]["stars"].map(star => {
                return `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`;
            }).join(', ');
        }

        // Concatenate the html tags with resultData jsonObject
        let rowHTML = "";
        rowHTML += "<tr>";
        rowHTML += "<td>" + '<a href="single-movie.html?id=' + resultData[i]['movie_id'] + '">'
            + resultData[i]["movie_title"] + '</a><br>' + "</td>";
        rowHTML += "<td>" + resultData[i]["movie_year"] + "</td>";
        rowHTML += "<td>" + resultData[i]["movie_director"] + "</td>";
        rowHTML += "<td>" + resultData[i]["genres"].join('<br>') + "</td>";
        rowHTML += "<td>" + stars + "</td>";
        rowHTML += "<td style='text-align: center;'>" + resultData[i]["rating"] + "</td>";
        rowHTML += "<td><img src='assets/gift.png' alt='Gift' style='width: 30px; height: 30px; margin-top: 0.5rem; margin-bottom: 0.5rem;'></td>";
        rowHTML += "</tr>";

        // Append the row created to the table body, which will refresh the page
        movieTableBodyElement.append(rowHTML);
    }
}


/**
 * Once this .js is loaded, following scripts will be executed by the browser
 */

// Makes the HTTP GET request and registers on success callback function handleMovieResult
jQuery.ajax({
    dataType: "json", // Setting return data type
    method: "GET", // Setting request method
    url: "api/movies", // Setting request url, which is mapped by MoviesServlet in MoviesServlet.java
    success: (resultData) => handleMovieResult(resultData) // Setting callback function to handle data returned successfully by the MoviesServlet
});