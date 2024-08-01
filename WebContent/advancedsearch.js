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

document.addEventListener('DOMContentLoaded', function() {
    const headingElement = document.getElementById('advancedsearch-heading');

    // Set the default heading to ensure consistency
    if (headingElement) {
        headingElement.innerText = "Results";
    }

    // Handle form submissions to prevent empty searches
    const advancedsearchForm = document.getElementById('advanced_search_form');
    if (advancedsearchForm) {
        advancedsearchForm.addEventListener('submit', function(event) {
            const titleInput = advancedsearchForm.querySelector('input[name="title"]').value.trim();
            const yearInput = advancedsearchForm.querySelector('input[name="year"]').value.trim();
            const directorInput = advancedsearchForm.querySelector('input[name="director"]').value.trim();
            const starInput = advancedsearchForm.querySelector('input[name="star"]').value.trim();

            if (titleInput === "" && yearInput === "" && directorInput === "" && starInput === "") {
                event.preventDefault();
                alert("Please enter at least one search criterion.");
            }
        });
    }

    const urlParams = new URLSearchParams(window.location.search);
    const queryString = new URLSearchParams({
        title: encodeURIComponent(urlParams.get("title") || ""),
        year: encodeURIComponent(urlParams.get("year") || ""),
        director: encodeURIComponent(urlParams.get("director") || ""),
        star: encodeURIComponent(urlParams.get("star") || "")
    }).toString();

    if (headingElement) {
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/advancedsearch?${queryString}`,
            success: (resultData) => handleAdvancedSearchResult(resultData),
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("AJAX request to /api/advancedsearch failed:", textStatus, errorThrown);
                alert("An error occurred while fetching advanced search results.");
            }
        });
    }
});

// Function to create clickable genre links
function createGenreLinks(genres) {
    return genres.map(genre => `<a href="genre.html?genre=${encodeURIComponent(genre)}">${genre}</a>`).join(', ');
}

// Function to handle the advanced search results and populate the movie table
function handleAdvancedSearchResult(resultData) {
    console.log("handleAdvancedSearchResult: populating movie table from resultData");

    const movieTableBodyElement = jQuery("#advancedsearch_table_body");
    console.log("Advanced Search results:", resultData.length);

    resultData.forEach((movie) => {
        let stars = [];
        const starsInfo = movie["stars"];

        if (Array.isArray(starsInfo)) {
            stars = starsInfo.map((starString) => {
                const starParts = starString.split('|');
                if (starParts.length === 2) {
                    return {
                        star_id: starParts[0],
                        star_name: starParts[1]
                    };
                } else {
                    console.warn("Unexpected format in 'stars':", starString);
                    return null;
                }
            }).filter(star => star !== null);

            const starLinks = stars.map((star) => `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`).join(', ');

            const genreLinks = createGenreLinks(movie["genres"]);

            const rowHTML = `
                <tr>
                    <td><a href="single-movie.html?id=${movie['movie_id']}">${movie["movie_title"]}</a></td>
                    <td>${movie["movie_year"]}</td>
                    <td>${movie["movie_director"]}</td>
                    <td>${genreLinks}</td>
                    <td>${starLinks}</td>
                    <td style='text-align: center;'>${movie["rating"]}</td>
                    <td>
                        <a href="shoppingcart.html?movie_id=${encodeURIComponent(movie['movie_id'])}&movie_title=${encodeURIComponent(movie["movie_title"])}">
                            <img src='assets/gift.png' style='width: 30px; height: 30px;' alt='Purchase Gift'>
                        </a>    
                    </td>
                </tr>
            `;

            movieTableBodyElement.append(rowHTML);
        } else {
            console.warn("starsInfo is not a valid array:", starsInfo);
        }
    });
}
