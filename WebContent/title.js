document.addEventListener('DOMContentLoaded', function () {
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const digits = '0123456789*';

    const titleDropdownContent = document.getElementById('title-dropdown-content');
    const headingElement = document.getElementById('title-heading');
    const defaultHeading = "Search Results";

    // Set default heading to ensure consistency
    if (headingElement) {
        headingElement.innerText = defaultHeading;
    }

    // Create dropdown row for A-Z
    const letterRow = document.createElement('div');
    letterRow.className = 'dropdown-row';
    letters.split('').forEach(letter => {
        const titleLink = document.createElement('a');
        titleLink.textContent = letter;

        titleLink.onclick = function () {
            // Navigate to the title page with the selected letter
            window.location.href = `title.html?letter=${encodeURIComponent(letter)}`;
        };

        letterRow.appendChild(titleLink); // Add to the row
    });

    titleDropdownContent.appendChild(letterRow); // Append the row to the dropdown content

    // Create dropdown row for 0-9 and *
    const digitRow = document.createElement('div');
    digitRow.className = 'dropdown-row';
    digits.split('').forEach(digit => {
        const digitLink = document.createElement('a');
        digitLink.textContent = digit;

        digitLink.onclick = function () {
            // Navigate to the title page with the selected digit
            window.location.href = `title.html?digit=${encodeURIComponent(digit)}`;
        };

        digitRow.appendChild(digitLink); // Add to the row
    });

    titleDropdownContent.appendChild(digitRow); // Append to the dropdown content

    // Determine if we are on the title page and check the selected letter or digit
    const urlParams = new URLSearchParams(window.location.search);
    const letter = urlParams.get("letter");
    const digit = urlParams.get("digit");

    // Set the heading dynamically based on the parameter
    let headingText = defaultHeading;

    if (letter && letter.trim() !== "") {
        headingText = letter.trim();
    } else if (digit && digit.trim() !== "") {
        headingText = digit.trim();
    } else {
        headingText = "Genres"; // Set heading to Genres if no specific letter or digit is selected
    }

    if (headingElement) {
        headingElement.innerText = headingText; // Set the heading
    }

    // Perform AJAX request to fetch movies based on the letter or digit
    const parameter = letter || digit; // Use whichever is defined

    if (parameter) {
        jQuery.ajax({
            dataType: "json",
            method: "GET",
            url: `api/title?content=${encodeURIComponent(parameter)}`, // Fetch movies by letter or digit
            success: (data) => handleTitleOrDigitResult(data), // Handle the returned data
            error: (jqXHR, textStatus, errorThrown) => {
                console.error("AJAX request to api/title failed:", textStatus, errorThrown);
                alert("An error occurred while fetching title information.");
            }
        });
    }
});

// Function to create genre links
function createGenreLinks(genres) {
    return genres.map(genre => `<a href="genre.html?genre=${encodeURIComponent(genre)}">${genre}</a>`).join(', ');
}

function handleTitleOrDigitResult(data) {
    console.log("handleTitleOrDigitResult: populating movie table from data");

    const movieTableBodyElement = jQuery("#title_table_body");
    console.log("Number of title results:", data.length);

    // Loop through the data to populate the table with movie information
    data.forEach((movie) => {
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

            const starLinks = stars.map((star) => {
                return `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`;
            }).join(', ');

            // Create genre links for each movie
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

            movieTableBodyElement.append(rowHTML); // Add the row to the table
        } else {
            console.warn("Unexpected format in 'stars':", starsInfo);
        }
    });
}
