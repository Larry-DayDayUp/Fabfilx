document.addEventListener('DOMContentLoaded', function () {
    // Get the dropdown content elements
    const genreDropdownContent = document.getElementById('genre-dropdown-content');
    const titleDropdownContent = document.getElementById('title-dropdown-content');

    // Fetch genres from the backend
    fetchGenres(genreDropdownContent, 'genre');

    // Populate title dropdown content with A-Z and 0-9
    const letters = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ';
    const digits = '0123456789';

    // Create row for A-Z
    const letterRow = document.createElement('div');
    letterRow.className = 'dropdown-row';
    populateDropdown(titleDropdownContent, letters.split(''), 'title');

    // Create row for 0-9
    const digitRow = document.createElement('div');
    digitRow.className = 'dropdown-row';
    populateDropdown(titleDropdownContent, digits.split(''), 'title');
});

function fetchGenres(container, type) {
    fetch(`api/genres?type=${type}`)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch genre data');
            }
            return response.json();
        })
        .then(genreData => {
            populateDropdown(container, genreData, type);
        })
        .catch(error => {
            console.error('Error fetching genre data:', error);
            alert('An error occurred while fetching genre data.');
        });
}

function populateDropdown(container, items, type) {
    items.forEach(item => {
        const link = document.createElement('a');
        link.textContent = item;
        link.onclick = function() {
            fetchMovies(type, item);
        };
        container.appendChild(link);
    });
}

function fetchMovies(type, content) {
    const url = type === 'genre' ? `api/genre?content=${encodeURIComponent(content)}` : `api/title?content=${encodeURIComponent(content)}`;
    fetch(url)
        .then(response => {
            if (!response.ok) {
                throw new Error('Failed to fetch movie data');
            }
            return response.json();
        })
        .then(movieData => {
            handleResult(movieData);
        })
        .catch(error => {
            console.error('Error fetching movie data:', error);
            alert('An error occurred while fetching movie data.');
        });
}

function handleResult(data) {
    console.log("handleResult: populating movie table from resultData");

    let movieTableBodyElement = jQuery("#genre_table_body");
    console.log("Results:", data.length);  // Debug output

    for (let i = 0; i < data.length; i++) {
        // Initialize an array to hold star objects
        let stars = [];
        let starsInfo = data[i]["stars"];

        if (Array.isArray(starsInfo)) {
            stars = starsInfo.map((starString) => {

                let starParts = starString.split('|');
                if (starParts.length === 2) {
                    return {
                        star_id: starParts[0],   // Extract star ID
                        star_name: starParts[1]  // Extract star name
                    };
                } else {
                    console.warn("Unexpected format in 'stars':", starString);  // Handle incorrect format
                    return null;
                }
            }).filter(star => star !== null);

            let starLinks = stars.map((star) => {
                return `<a href="single-star.html?id=${encodeURIComponent(star.star_id)}">${star.star_name}</a>`;
            }).join(', ');

            // Build the row for the movie table
            let rowHTML = "<tr>";

            rowHTML += `<td><a href="single-movie.html?id=${data[i]['movie_id']}">${data[i]["movie_title"]}</a></td>`;
            rowHTML += `<td>${data[i]["movie_year"]}</td>`;
            rowHTML += `<td>${data[i]["movie_director"]}</td>`;
            rowHTML += `<td>${data[i]["genres"].join('<br>')}</td>`;
            rowHTML += `<td>${starLinks}</td>`;  // Add clickable star links to the table row
            rowHTML += `<td style='text-align: center;'>${data[i]["rating"]}</td>`;
            rowHTML += `<td><img src='assets/gift.png' style='width: 30px; height: 30px;'></td>`;
            rowHTML += "</tr>";

            // Append the row to the table body
            movieTableBodyElement.append(rowHTML);
        } else {
            console.warn("starsInfo is not a valid array:", starsInfo);  // Handle unexpected cases
        }
    }
}
