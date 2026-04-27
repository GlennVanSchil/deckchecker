function setupDuplicateFilters() {
    setupTableFilters({
        searchInputId: "duplicateSearch",
        minInputId: "duplicateMin",
        tableBodyId: "duplicateTableBody",
        filterInfoId: "duplicateFilterInfo",
        minDataField: "duplicates",
        textDataFields: ["cardNumber", "cardName"],
        label: "duplicate cards",
    });
    setupTableFilters({
        searchInputId: "combinedVariantsSearch",
        minInputId: "combinedVariantsMin",
        tableBodyId: "combinedVariantsTableBody",
        filterInfoId: "combinedVariantsFilterInfo",
        minDataField: "overflow",
        textDataFields: ["duplicateCard", "name", "variantGroup", "additional"],
        label: "combined-variant duplicate rows",
    });
    setupTableFilters({
        searchInputId: "variantGroupOverflowSearch",
        minInputId: "variantGroupOverflowMin",
        tableBodyId: "variantGroupOverflowTableBody",
        filterInfoId: "variantGroupOverflowFilterInfo",
        minDataField: "overflow",
        textDataFields: ["variantGroup", "name", "ownedVariants"],
        label: "variant-group overflow rows",
    });
}

function setupTableFilters(config) {
    const searchInput = document.getElementById(config.searchInputId);
    const minInput = document.getElementById(config.minInputId);
    const tableBody = document.getElementById(config.tableBodyId);
    const filterInfo = document.getElementById(config.filterInfoId);
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
        const minValue = Number.isFinite(parsedMin) ? Math.max(parsedMin, 1) : 1;

        let visible = 0;
        for (const row of rows) {
            const searchable = config.textDataFields
                .map((field) => (row.dataset[field] || "").toLowerCase())
                .join(" ");
            const numericValue = Number.parseInt(row.dataset[config.minDataField] || "0", 10);

            const matchesQuery = query.length === 0 || searchable.includes(query);
            const matchesMin = numericValue >= minValue;
            const show = matchesQuery && matchesMin;
            row.style.display = show ? "" : "none";
            if (show) {
                visible++;
            }
        }

        filterInfo.textContent = `Showing ${visible} of ${totalRows} ${config.label}`;
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
