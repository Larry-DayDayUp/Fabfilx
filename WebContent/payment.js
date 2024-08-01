document.getElementById("place_order").onclick = function() {
    const firstName = document.getElementById("first_name").value;
    const lastName = document.getElementById("last_name").value;
    const cardNumber = document.getElementById("card_number").value;
    const expirationDate = document.getElementById("expiration_date").value;

    // Validate the format of the expiration date
    const dateRegex = /^\d{4}-\d{2}-\d{2}$/; // YYYY-MM-DD
    if (!expirationDate.match(dateRegex)) {
        alert("Expiration date must be in YYYY-MM-DD format.");
        return;
    }

    // Ensure all required fields are filled
    if (!firstName || !lastName || !cardNumber || !expirationDate) {
        alert("Please fill in all required fields.");
        return;
    }

    // Make a POST request to the backend to place the order
    jQuery.ajax({
        dataType: "json",
        method: "POST", // POST request
        url: "api/place-order", // Correct endpoint
        data: {
            first_name: firstName,
            last_name: lastName,
            card_number: cardNumber,
            expiration_date: expirationDate,
        },
        success: function(response) {
            document.getElementById("order_result").innerText = "Order placed successfully!";
            window.location.href = "order-confirmation.html"; // Redirect to the confirmation page
        },
        error: function(jqXHR, textStatus, errorThrown) {
            document.getElementById("order_result").innerText = "Invalid credit card information or error placing order. Please try again.";
        },
    });
};

// Set the total price on the payment page
const totalPrice = sessionStorage.getItem("totalPrice") || "0.00"; // Get the total price from session storage
document.getElementById("total_price").innerText = `$${parseFloat(totalPrice).toFixed(2)}`; // Display the total price