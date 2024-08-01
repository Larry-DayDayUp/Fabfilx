$(document).ready(function() {
    // Fetch and display database metadata
    $.ajax({
        url: "api/metadata",
        type: "GET",
        success: function(response) {
            const metadata = JSON.parse(response);
            const tables = metadata.tables;
            console.log(metadata);
            // Create a container for displaying the metadata
            const metadataContainer = $("#metadata-container");

            tables.forEach(table => {
                // Create a table element
                const tableElement = $("<table>").addClass("table table-bordered");
                const tableHeader = $("<thead>").append("<tr><th>Column Name</th><th>Type</th><th>Size</th><th>Nullable</th></tr>");
                const tableBody = $("<tbody>");

                const tableTitle = $("<h3>").text(`Table: ${table.tableName}`);
                metadataContainer.append(tableTitle);

                table.columns.forEach(column => {
                    // Create table rows with column information
                    const row = $("<tr>");
                    row.append($("<td>").text(column.columnName));
                    row.append($("<td>").text(column.dataType));
                    row.append($("<td>").text(column.columnSize));
                    row.append($("<td>").text(column.nullable));
                    tableBody.append(row);
                });

                tableElement.append(tableHeader, tableBody);
                metadataContainer.append(tableElement); // Append the table to the container
            });
        },
        error: function(xhr, status, error) {
            console.error("Error retrieving metadata:", error); // Log any errors
        }
    });
});
