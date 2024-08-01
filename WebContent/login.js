let customer_form = $("#customer_form");

/**
 * Handle the data returned by LoginServlet
 * @param resultDataString jsonObject
 */
function handleCustomerLoginResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("handle customer login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // Get the email entered by the user
    const customerEmail = $("#customer_email").val(); // Adjust if the ID of the email input field is different
    console.log(customerEmail);

    // If login succeeds, it will redirect the user to mainpage.html
    if (resultDataJson["status"] === "success") {
        // Redirect the employee to the mainpage on successful login
        window.location.replace("mainpage.html");
    } else {
        console.log("Show error message");
        console.log(resultDataJson["message"]);

        // Reset styles of errorText
        let errorText = $("#customer_login_error_message"); // Updated ID for error message display
        errorText.css({
            color: "white",
            display: "block",
            fontSize: "0.9rem",
            fontFamily: "sans-serif",
            backgroundColor: "#E50815",
            borderRadius: "0.3rem",
            padding: "0.8rem"
        });

        // Display specific error messages
        if (resultDataJson["message"] === "Incorrect password") {
            errorText.text("Incorrect password for " + customerEmail + ".");
        } else {
            errorText.text("Sorry, we can't find an account with this email address.");
        }
        grecaptcha.reset()
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent
 */
function submitCustomerLoginForm(formSubmitEvent) {
    console.log("submit customer login form");
    // Prevent the default form submission behavior
    formSubmitEvent.preventDefault();

    // Delay to ensure reCAPTCHA response is generated

    const recaptchaResponse = $("#customer_reCAPTCHA .g-recaptcha-response").val(); // Corrected selector
    if (!recaptchaResponse || recaptchaResponse.trim() === "") {
        console.error("reCAPTCHA not completed or empty.");
        alert("Please complete the reCAPTCHA challenge."); // Ask user to complete reCAPTCHA
        return; // Stop form submission if reCAPTCHA isn't completed
    }

    // Store the reCAPTCHA response in sessionStorage
    sessionStorage.setItem("recaptchaResponse", recaptchaResponse);

    // Continue with form submission
    const formData = customer_form.serialize() + "&g-recaptcha-response=" + recaptchaResponse;
    
    $.ajax(
        "api/login", {
            method: "POST",
            data: formData,
            success: handleCustomerLoginResult, // Handle server response
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Error during login:", textStatus, errorThrown);
                alert("An error occurred during the login process. Please try again.");
            },
        }
    );
}

// Bind the submit action of the form to the `submitLoginForm` function
customer_form.submit(submitCustomerLoginForm);