$(document).ready(function() {
    // Function to create a formatted success prompt
    function createPrompt(message) {
        const prompt = $("<div>")
            .addClass("success-prompt")
            .hide()
            .fadeIn(500); // Smooth appearance

        prompt.text(message); // Set the success message

        $("#movieForm").after(prompt); // Position the prompt below the form

        // Automatically remove the success prompt after 1 minute
        setTimeout(() => {
            prompt.fadeOut(500, () => {
                prompt.remove(); // Remove the prompt after fading
            });
        }, 60000); // 1 minute
    }

    // Form for inserting a new star
    $("#starForm").submit(function(event) {
        event.preventDefault(); // Prevent default form submission

        const starName = $("#starName").val();
        const starBirthYear = $("#starBirthYear").val();

        if (!starName) {
            alert("Please enter the star's name.");
            return;
        }

        $.ajax({
            url: "api/operation",
            type: "POST",
            data: {
                action: "insert_star",
                starName: starName,
                starBirthYear: starBirthYear
            },
            success: function(response) {
                // Check if the response indicates that the star already exists
                console.log(response);
                if (response.startsWith("Star already exists with ID: ")) {
                    createPrompt(response) // Show the duplicate prompt
                } else {
                    // Format the success message for the star
                    const starID = response.split(": ")[1];
                    const successMessage = `Successfully added: Star(ID="${starID}", name="${starName}", birthyear=${starBirthYear || "N/A"})`;
                    createPrompt(successMessage); // Show the success prompt
                }
            },
            error: function(xhr, status, error) {
                console.error("Error inserting star:", error);
                alert("Error inserting star. Please try again.");
            }
        });
    });

    // Form for adding a new movie
    $("#movieForm").submit(function(event) {
        event.preventDefault(); // Prevent default form submission

        const movieTitle = $("#movieTitle").val();
        const releaseYear = $("#releaseYear").val();
        const director = $("#director").val();
        const starName = $("#starNameMovie").val();
        const genreName = $("#genreName").val();

        if (!movieTitle || !releaseYear || !director || !starName || !genreName) {
            alert("Please fill in all required fields.");
            return;
        }

        $.ajax({
            url: "api/operation",
            type: "POST",
            data: {
                action: "add_movie",
                movieTitle: movieTitle,
                releaseYear: releaseYear,
                director: director,
                starName: starName,
                genreName: genreName
            },
            success: function(response) {
                console.log(response);
                // Check if the response indicates that the movie already exists
                if (response.startsWith("Movie already exists with ID: ")) {
                    console.log(response);
                    createPrompt(response); // Show the duplicate prompt
                } else {
                    console.log(response);
                    // Extract movie ID, genre ID, and star ID from the response
                    const parts = response.split(", ");
                    const movieID = parts[0].split(": ")[1].trim();
                    const genreID = parts[1].split(": ")[1].trim();
                    const starID = parts[2].split(": ")[1].trim();
                    // Format the success message for the movie
                    const successMessage = `Successfully added: Movie(ID="${movieID}", title="${movieTitle}", director="${director}", year=${releaseYear}, genre(ID="${genreID}"), star(ID="${starID}", name="${starName}"))`;
                    createPrompt(successMessage); // Show the success prompt
                }
            },
            error: function(xhr, status, error) {
                console.error("Error adding movie:", error);
                alert("Error adding movie. Please try again.");
            }
        });
    });
});
