document.addEventListener("DOMContentLoaded", function() {
    let shoppingCart = JSON.parse(sessionStorage.getItem("shoppingCart") || "[]");
    const cartTableBody = jQuery("#cart_table_body");
    const checkoutButton = document.getElementById("checkout_button");
    const emptyCartMessage = document.getElementById("empty_cart_message");
    const totalPriceElement = jQuery("#total_price"); // Total price display
    const proceedToPaymentButton = jQuery("#proceed_to_payment");

    const urlParams = new URLSearchParams(window.location.search);
    const movieId = urlParams.get("movie_id");
    const movieTitle = urlParams.get("movie_title");

    if (movieId && movieTitle) {
        addToCart(movieId, movieTitle); // Add the movie to the cart
    }

    function addToCart(movieId, movieTitle) {
        const existingItemIndex = shoppingCart.findIndex((item) => item.movie_id === movieId);

        if (existingItemIndex !== -1) {
            const existingItem = shoppingCart[existingItemIndex];
            existingItem.quantity++;
            const price = parseFloat(existingItem.price);
            existingItem.subtotal = (price * existingItem.quantity).toFixed(2);
        } else {
            // New movie to be added
            jQuery.ajax({
                dataType: "json",
                method: "POST",
                url: "api/cart",
                data: { movie_id: movieId, movie_title: movieTitle },
                success: function(response) {
                    const price = parseFloat(response.price);
                    const subtotal = (price).toFixed(2);

                    const newItem = {
                        movie_id: response.movie_id,
                        movie_title: response.movie_title,
                        quantity: 1,
                        price: price,
                        subtotal: parseFloat(subtotal),
                    };

                    shoppingCart.push(newItem);
                    sessionStorage.setItem("shoppingCart", JSON.stringify(shoppingCart)); // Store in session storage
                    renderShoppingCart(); // Refresh the cart display
                },
                error: function(jqXHR, textStatus, errorThrown) {
                    console.error("Error adding movie to cart:", textStatus, errorThrown);
                    alert("Failed to add movie. Please try again.");
                },
            });
        }

        renderShoppingCart(); // Refresh the cart display
    }

    function renderShoppingCart() {
        cartTableBody.empty(); // Clear existing content

        shoppingCart.forEach((item) => {
            const { movie_id, movie_title, quantity, price, subtotal } = item;

            const rowHTML = `
                <tr data-movie-id="${movie_id}">
                    <td>${movie_title}</td> <!-- Title -->
                    <td>
                        <input type="number" value="${quantity}" min="1" class="quantity-input" data-movie-id="${movie_id}" style="text-align: center;">
                    </td>
                    <td>$${parseFloat(price).toFixed(2)}</td> <!-- Price -->
                    <td>$${parseFloat(subtotal).toFixed(2)}</td> <!-- Subtotal -->
                    <td><button class="remove-button" data-movie-id="${movie_id}">Remove</button></td> <!-- Separate column for "Remove" -->
                </tr>
            `;

            cartTableBody.append(rowHTML);
        });

        updateTotalPrice(); // Recalculate the total price
        setupEventListeners(); // Set event listeners for quantity changes and removals
    }

    function updateTotalPrice() {
        const totalPrice = shoppingCart.reduce((total, item) => {
            return total + parseFloat(item.subtotal);
        }, 0);

        // Update the total price display
        totalPriceElement.text(`Total: $${totalPrice.toFixed(2)}`);

        // Store the total price in session storage
        sessionStorage.setItem("totalPrice", totalPrice.toFixed(2)); // Store as a string with two decimal places
    }

    function setupEventListeners() {
        addQuantityChangeListeners(); // Event listeners for quantity changes
        addRemoveButtonClickListeners(); // Event listeners for item removals
    }

    function addQuantityChangeListeners() {
        cartTableBody.find(".quantity-input").each(function() {
            jQuery(this).on("change", function() {
                const movieId = jQuery(this).data("movie-id");
                const newQuantity = parseInt(jQuery(this).val(), 10);

                if (isNaN(newQuantity) || newQuantity < 1) {
                    alert("Quantity must be a positive integer.");
                    return;
                }

                const movie = shoppingCart.find((item) => item.movie_id === movieId);

                if (movie) {
                    movie.quantity = newQuantity;
                    movie.subtotal = (movie.price * newQuantity).toFixed(2);

                    sessionStorage.setItem("shoppingCart", JSON.stringify(shoppingCart)); // Update session storage
                    renderShoppingCart(); // Refresh the cart display
                }
            });
        });
    }

    function addRemoveButtonClickListeners() {
        cartTableBody.find(".remove-button").each(function() {
            jQuery(this).on("click", function() {
                const movieId = jQuery(this).data("movie-id");
                const movieIndex = shoppingCart.findIndex((item) => item.movie_id === movieId);

                if (movieIndex !== -1) {
                    shoppingCart.splice(movieIndex, 1); // Remove from the cart
                    sessionStorage.setItem("shoppingCart", JSON.stringify(shoppingCart)); // Update session storage
                    renderShoppingCart(); // Refresh the cart display
                }
            });
        });
    }

    proceedToPaymentButton.on("click", function() {
        window.location.href = "payment.html"; // Redirect to payment page
    });

    renderShoppingCart(); // Refresh the cart display
});