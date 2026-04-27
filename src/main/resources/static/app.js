function setupDuplicateFilters() {
    const searchInput = document.getElementById("duplicateSearch");
    const minInput = document.getElementById("duplicateMin");
    const tableBody = document.getElementById("duplicateTableBody");
    const filterInfo = document.getElementById("duplicateFilterInfo");

    if (!searchInput || !minInput || !tableBody || !filterInfo) {
        return;
    }

    if (tableBody.dataset.filtersInitialized === "true") {
        return;
    }
    tableBody.dataset.filtersInitialized = "true";

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

function setupTabs() {
    if (document.body.dataset.tabsInitialized === "true") {
        return;
    }
    document.body.dataset.tabsInitialized = "true";

    const tabButtons = Array.from(document.querySelectorAll(".tab-btn"));
    const tabPanels = Array.from(document.querySelectorAll(".tab-panel"));
    if (tabButtons.length === 0 || tabPanels.length === 0) {
        return;
    }

    const activateTab = (targetId) => {
        for (const button of tabButtons) {
            const active = button.dataset.tabTarget === targetId;
            button.classList.toggle("active", active);
            button.setAttribute("aria-selected", active ? "true" : "false");
        }
        for (const panel of tabPanels) {
            panel.classList.toggle("active", panel.id === targetId);
        }
    };

    for (const button of tabButtons) {
        button.addEventListener("click", () => activateTab(button.dataset.tabTarget));
    }

    const defaultButton = tabButtons.find((button) => button.classList.contains("active")) || tabButtons[0];
    activateTab(defaultButton.dataset.tabTarget);
}

function initializePageFeatures() {
    setupTabs();
    setupDuplicateFilters();
}

document.addEventListener("DOMContentLoaded", initializePageFeatures);
window.addEventListener("pageshow", initializePageFeatures);
