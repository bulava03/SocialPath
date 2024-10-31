function dropdownUser() {
    var userIcon = document.getElementById('usericon');
    var dropdown = document.getElementById('dropdown');

    dropdown.classList.toggle('show'); // Додаємо або прибираємо клас show
    userIcon.style.backgroundColor = dropdown.classList.contains('show') ? '#fa9434' : ''; // Змінюємо фон
}

window.onclick = function (event) {
    var userIcon = document.getElementById('usericon');
    var dropdown = document.getElementById('dropdown');

    if (!event.target.matches('#usericon')) {
        // Закриваємо меню, якщо клікнули поза usericon
        if (dropdown.classList.contains('show')) {
            dropdown.classList.remove('show');
            userIcon.style.backgroundColor = ''; // Прибираємо фон
        }
    }
}

function personalInformation() {
    document.getElementById('formbackPersonnalInfo').submit();
}

function groupCreation() {
    document.getElementById('formbackGroupCreation').submit();
}
