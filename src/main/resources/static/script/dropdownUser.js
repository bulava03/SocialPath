function dropdownUser() {
    var userIcon = document.getElementById('usericon');
    var dropdown = document.getElementById('dropdown');

    dropdown.classList.toggle('show'); // Toggle the show class
    userIcon.style.backgroundColor = dropdown.classList.contains('show') ? '#fa9434' : ''; // Change the background
}

window.onclick = function (event) {
    var userIcon = document.getElementById('usericon');
    var dropdown = document.getElementById('dropdown');

    if (!event.target.matches('#usericon')) {
        // Close the menu when clicking outside the usericon
        if (dropdown.classList.contains('show')) {
            dropdown.classList.remove('show');
            userIcon.style.backgroundColor = ''; // Reset the background
        }
    }
}
