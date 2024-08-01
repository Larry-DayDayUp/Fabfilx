// Default settings for pagination
let resultsPerPage = 10; // Default results per page
let currentPage = 1; // Default to first page

document.addEventListener('DOMContentLoaded', function() {
    const genreDropdownContent = document.getElementById('genre-dropdown-content');
    const headingElement = document.getElementById('genre-heading');
    const defaultHeading = "Search Results";

    const updateGenreDropdown = (genres) => {
        genreDropdownContent.innerHTML = ""; // Clear existing dropdown content
        // Populate the genre dropdown with genres in rows of 10
        for (let i = 0; i < genres.length; i += 10) {
            const row = document.createElement('div');
            row.className = 'dropdown-row';

            genres.slice(i, i + 10).forEach(genre => {
                const genreLink = document.createElement('a');
                genreLink.textContent = genre;
                genreLink.onclick = function() {
                    window.location.href = `genre.html?genre=${encodeURIComponent(genre)}`;
                };
                row.appendChild(genreLink);
            });

            genreDropdownContent.appendChild(row);
        }
    };

    // Fetch genres from backend and populate dropdown
    fetchGenres();

    function fetchGenres() {
        fetch('api/retrieveGenres')
            .then(response => {
                if (!response.ok) {
                    throw new Error('Failed to fetch genre list');
                }
                return response.json();
            })
            .then(genres => {
                updateGenreDropdown(genres);
            })
            .catch(error => {
                console.error('Error fetching genre list:', error);
                alert('An error occurred while fetching genre list.');
            });
    }

    const updatePaginationControls = () => {
        document.getElementById("prev-button").disabled = currentPage <= 1;
        fetchSearchResultsGenres();
    };

    const fetchSearchResultsGenres = () => {
        const genreParams = new URLSearchParams(window.location.search);
        const genreName = genreParams.get("genre");
        let headingText = genreName && genreName.trim() !== "" ? genreName.trim() : "Invalid search query";
        headingElement.innerText = headingText;  // Update the heading text

        if (genreName) {
            jQuery.ajax({
                dataType: "json",
                method: "GET",
                url: `api/genre?content=${encodeURIComponent(genreName)}&resultsPerPage=${resultsPerPage}&currentPage=${currentPage}`,
                success: (resultData) => {
                    handleGenreResult(resultData);
                    updateNextButton(resultData); // Check if 'Next' should be enabled or disabled
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    console.error("AJAX request to api/genre failed:", textStatus, errorThrown);
                    alert("An error occurred while fetching genre information.");
                }
            });
        }
    };

    const updateNextButton = (resultData) => {
        const nextButton = document.getElementById("next-btn");

        // If the number of results is less than 'resultsPerPage', we're likely at the end
        nextButton.disabled = resultData.length < resultsPerPage;
        console.log(resultData.length, resultsPerPage);
        console.log("Button disabled state:", nextButton.disabled);
    };

    // Event listener for "Prev" button
    document.getElementById("prev-button").addEventListener("click", function() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationControls();
        }
    });

    // Event listener for "Next" button
    document.getElementById("next-btn").addEventListener("click", function() {
        if (!this.disabled) {
            currentPage++;
            updatePaginationControls();
        }
    });

    // Event listener for drop-down menu (results per page)
    document.getElementById("results-per-page").addEventListener("change", function() {
        resultsPerPage = parseInt(this.value, 10);
        currentPage = 1; // Reset to the first page
        updatePaginationControls();
    });

    // Initialize by fetching the first set of results
    updatePaginationControls();
});

function handleGenreResult(genreData) {
    const movieTableBodyElement = jQuery("#genre_table_body");
    movieTableBodyElement.empty();

    const hasExtraItem = genreData.length > resultsPerPage;
    const displayData = hasExtraItem ? genreData.slice(0, resultsPerPage) : genreData;

    // Loop through genreData to populate the table
    genreData.forEach((movie) => {
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
                    console.warn("Unexpected format in 'stars':", starString);  // Handle incorrect format
                    return null;
                }
            }).filter((star) => star !== null);

            const starLinks = stars.map((star) => {
                return `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`;
            }).join(', ');

            const rowHTML = `
                <tr>
                    <td><a href="single-movie.html?id=${movie['movie_id']}">${movie["movie_title"]}</a></td>
                    <td>${movie["movie_year"]}</td>
                    <td>${movie["movie_director"]}</td>
                    <td>${movie["genres"].map(genre => `<a href="genre.html?genre=${encodeURIComponent(genre)}">${genre}</a>`).join('<br>')}</td>
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
            console.warn("starsInfo is not a valid array:", starsInfo); // Handle unexpected cases
        }
    });
}
