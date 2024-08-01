let employeeForm = $("#employee_form");

/**
 * Handle the data returned by EmployeeLoginServlet
 * @param resultDataString JSON object
 */
function handleEmployeeLoginResult(resultDataString) {
    let resultDataJson = JSON.parse(resultDataString);

    console.log("Handle employee login response");
    console.log(resultDataJson);
    console.log(resultDataJson["status"]);

    // Get the email entered by the employee
    const employeeEmail = $("#employee_email").val(); // Updated ID for email input
    console.log(employeeEmail);

    if (resultDataJson["status"] === "success") {
        // Redirect the employee to the dashboard on successful login
        window.location.replace("dashboard.html");
    } else {
        console.log("Show error message");
        console.log(resultDataJson["message"]);

        // Update error message styling
        let errorText = $("#employee_login_error_message"); // Updated ID for error message display
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
            errorText.text("Incorrect password for " + employeeEmail + ".");
        } else {
            errorText.text("Sorry, we can't find an account with this email address.");
        }
        grecaptcha.reset()
    }
}

/**
 * Submit the form content with POST method
 * @param formSubmitEvent Event to be prevented
 */
function submitEmployeeLoginForm(formSubmitEvent) {
    console.log("Submit employee login form");
    formSubmitEvent.preventDefault(); // Prevent default form submission

    // Delay to ensure reCAPTCHA response is generated

    const recaptchaResponse = $("#employee_reCAPTCHA .g-recaptcha-response").val(); // Get reCAPTCHA value

    if (!recaptchaResponse || recaptchaResponse.trim() === "") {
        console.error("reCAPTCHA not completed or empty.");
        alert("Please complete the reCAPTCHA challenge."); // Ask user to complete reCAPTCHA
        return; // Stop form submission if reCAPTCHA isn't completed
    }

    // Store the reCAPTCHA response in sessionStorage
    sessionStorage.setItem("recaptchaResponse", recaptchaResponse);

    // Continue with form submission
    const formData = employeeForm.serialize() + "&g-recaptcha-response=" + recaptchaResponse;

    $.ajax(
        "api/employee_login", {
            method: "POST",
            data: formData,
            success: handleEmployeeLoginResult, // Handle server response
            error: function (jqXHR, textStatus, errorThrown) {
                console.error("Error during employee login:", textStatus, errorThrown);
                alert("An error occurred during the login process. Please try again.");
            },
        }
    );
}


// Bind the form's submit action to the `submitEmployeeLoginForm` function
employeeForm.submit(submitEmployeeLoginForm);