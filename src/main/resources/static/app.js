function setupDuplicateFilters() {
    const searchInput = document.getElementById("duplicateSearch");
    const minInput = document.getElementById("duplicateMin");
    const tableBody = document.getElementById("duplicateTableBody");
    const filterInfo = document.getElementById("duplicateFilterInfo");

    if (!searchInput || !minInput || !tableBody || !filterInfo) {
        return;
    }

    const rows = Array.from(tableBody.querySelectorAll("tr"));
    const totalRows = rows.length;

    const applyFilters = () => {
        const query = searchInput.value.trim().toLowerCase();
        const parsedMin = Number.parseInt(minInput.value || "1", 10);
        const minDuplicates = Number.isFinite(parsedMin) ? Math.max(parsedMin, 1) : 1;

        let visible = 0;
        for (const row of rows) {
            const card = (row.dataset.cardNumber || "").toLowerCase();
            const name = (row.dataset.cardName || "").toLowerCase();
            const duplicates = Number.parseInt(row.dataset.duplicates || "0", 10);

            const matchesQuery = query.length === 0 || card.includes(query) || name.includes(query);
            const matchesMin = duplicates >= minDuplicates;

            const show = matchesQuery && matchesMin;
            row.style.display = show ? "" : "none";
            if (show) {
                visible++;
            }
        }

        filterInfo.textContent = `Showing ${visible} of ${totalRows} duplicate cards`;
    };

    searchInput.addEventListener("input", applyFilters);
    minInput.addEventListener("input", applyFilters);
    applyFilters();
}

document.addEventListener("DOMContentLoaded", setupDuplicateFilters);
window.addEventListener("pageshow", setupDuplicateFilters);
