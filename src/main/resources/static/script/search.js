function removeExtraSpaces(input) {
    if (!input.trim()) {
        return { transformedString: '', isEmptyOrContainsOnlySpaces: true };
    }

    const transformedString = input.replace(/\s+/g, ' ');
    return { transformedString, isEmptyOrContainsOnlySpaces: false };
}

function submitSearch(event) {
    event.preventDefault();
    const pseudoSearchInput = document.getElementById('pseudoSearchInput');
    const searchInput = document.getElementById('searchInput');

    const input = pseudoSearchInput.value;
    const { transformedString, isEmptyOrContainsOnlySpaces } = removeExtraSpaces(input);

    if (isEmptyOrContainsOnlySpaces === false) {
        searchInput.value = transformedString;
        document.getElementById('searchForm').submit();
    }
}
