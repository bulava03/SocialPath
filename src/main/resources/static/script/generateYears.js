// Fills the year select with 1900..current, skipping a year that is already
// present as the pre-selected option rendered by the server.
(function () {
    const selectElement = document.getElementById("dob-year");
    if (!selectElement) {
        return;
    }
    const existing = new Set(
        Array.from(selectElement.options).map(option => option.value));

    const currentYear = new Date().getFullYear();
    for (let year = currentYear; year >= 1900; year--) {
        if (existing.has(String(year))) {
            continue;
        }
        const optionElement = document.createElement("option");
        optionElement.value = year;
        optionElement.text = year;
        selectElement.add(optionElement);
    }
})();
