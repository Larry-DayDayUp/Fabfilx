document.addEventListener("DOMContentLoaded", function() {
    const headingElement = document.getElementById("searchbar-heading");
    const defaultHeading = "Search Results";
    const searchInput = document.getElementById("autocomplete");

    // Default settings for pagination
    let resultsPerPage = 10; // Default results per page
    let currentPage = 1; // Default to first page

    const updatePaginationControls = () => {
        // Disable 'Prev' button on the first page
        document.getElementById("prev-button").disabled = currentPage <= 1;

        // Make AJAX request to fetch the current page
        fetchSearchResults();
    };

    const fetchSearchResults = () => {
        const query = new URLSearchParams(window.location.search).get("query") || "";
        let headingText = defaultHeading;  // Default heading text

        if (query && query.trim() !== "") {
            headingText = `Search Results for "${query.trim()}"`;  // Update heading with the query
        }

        if (headingElement) {
            headingElement.innerText = headingText;  // Update the heading text
        }

        // Perform AJAX request with pagination parameters
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/searchbar?query=${encodeURIComponent(query)}&resultsPerPage=${resultsPerPage}&currentPage=${currentPage}`,
            success: (resultData) => {
                handleSearchBarResult(resultData);
                updateNextButton(resultData); // Check if 'Next' should be enabled or disabled
            },
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("AJAX request to api/searchbar failed:", textStatus, errorThrown);
                alert("An error occurred while fetching search results.");
            }
        });
    };

    const updateNextButton = (resultData) => {
        const nextButton = document.getElementById("next-button");

        // If the number of results is less than 'resultsPerPage', we're likely at the end
        nextButton.disabled = resultData.length < resultsPerPage;
    };

    // Event listener for "Prev" button
    document.getElementById("prev-button").addEventListener("click", function() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationControls();
        }
    });

    // Event listener for "Next" button
    document.getElementById("next-button").addEventListener("click", function() {
        currentPage++;
        updatePaginationControls();
    });

    // Event listener for drop-down menu (results per page)
    document.getElementById("results-per-page").addEventListener("change", function() {
        resultsPerPage = parseInt(this.value, 10);
        currentPage = 1; // Reset to the first page
        updatePaginationControls();
    });

    // Initialize by fetching the first set of results
    updatePaginationControls();

    function handleLookup(query, doneCallback) {
        console.log("Autocomplete initiated");
        console.log("Sending AJAX request to backend Java Servlet");

        jQuery.ajax({
            "method": "GET",
            "url": `api/autocomplete?query=${escape(query)}`,
            "success": function(data) {
                handleLookupAjaxSuccess(data, query, doneCallback);
            },
            "error": function(errorData) {
                console.log("Lookup AJAX error");
                console.log(errorData);
            }
        });
    }

    function handleLookupAjaxSuccess(data, query, doneCallback) {
        console.log("Lookup AJAX successful");

        // parse the string into JSON
        var jsonData = JSON.parse(data);
        console.log(jsonData);

        // call the callback function provided by the autocomplete library
        doneCallback({ suggestions: jsonData });
    }

    function handleSelectSuggestion(suggestion) {
        // Perform a search with the selected suggestion
        console.log("You selected " + suggestion["value"] + " with ID " + suggestion["data"]["movie_id"]);
        searchInput.value = suggestion["value"];
        $('#search_form').submit(); // Submit the form with the selected suggestion
    }

    // Autocomplete functionality
    $('#autocomplete').autocomplete({
        lookup: function (query, doneCallback) {
            handleLookup(query, doneCallback);
        },
        onSelect: function (suggestion) {
            handleSelectSuggestion(suggestion);
        },
        deferRequestBy: 300, // Delay in milliseconds
        minChars: 3 // Minimum characters before autocomplete starts
    });

    // Handle normal search if no suggestion is selected
    function handleNormalSearch(query) {
        console.log("Performing normal search with query: " + query);
        window.location.href = `searchbar.html?query=${encodeURIComponent(query)}`;
    }

    // Bind pressing enter key to a handler function
    $('#autocomplete').keypress(function(event) {
        if (event.keyCode === 13) {
            handleNormalSearch($('#autocomplete').val());
        }
    });

    // Optional: Bind the search button click event
    $('#search_form').submit(function(event) {
        event.preventDefault(); // Prevent the default form submission
        handleNormalSearch($('#autocomplete').val());
    });

});

function handleSearchBarResult(resultData) {
    let movieTableBodyElement = jQuery("#searchbar_table_body");
    movieTableBodyElement.empty();

    resultData.forEach((movie) => {
        let stars = [];
        let starsInfo = movie["stars"];

        if (Array.isArray(starsInfo)) {
            stars = starsInfo.map((starString) => {
                let starParts = starString.split('|');
                if (starParts.length === 2) {
                    return {
                        star_id: starParts[0],
                        star_name: starParts[1]
                    };
                } else {
                    console.warn("Unexpected format in 'stars':", starString);
                    return null;
                }
            }).filter((star) => star !== null);

            let starLinks = stars.map((star) => `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`).join(', ');

            // Build the row for the movie table
            let rowHTML = `<tr>
                <td><a href="single-movie.html?id=${movie['movie_id']}">${movie["movie_title"]}</a></td>
                <td>${movie["movie_year"]}</td>
                <td>${movie["movie_director"]}</td>
                <td>${movie["genres"].map((g) => `<a href="genre.html?genre=${encodeURIComponent(g)}">${g}</a>`).join(', ')}</td>
                <td>${starLinks}</td>
                <td style='text-align: center;'>${movie["rating"]}</td>
                <td>
                    <a href="shoppingcart.html?movie_id=${encodeURIComponent(movie['movie_id'])}&movie_title=${encodeURIComponent(movie["movie_title"])}">
                        <img src='assets/gift.png' style='width: 30px; height: 30px;' alt='Purchase Gift'>
                    </a>    
                </td>
            </tr>`;

            movieTableBodyElement.append(rowHTML);
        }
    });
}