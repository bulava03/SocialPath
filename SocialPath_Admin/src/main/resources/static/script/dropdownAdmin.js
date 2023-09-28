function dropdownAdmin() {
    document.getElementById('dropdown').classList.toggle('show');
}

window.onclick = function (event) {
    if (!event.target.matches('#usericon')) {
        var dropdowns = document.getElementsByClassName('dropdown');
        for (var i = 0; i < dropdowns.length; i++) {
            var opendropdown = dropdowns[i];
            if (opendropdown.classList.contains('show')) {
                opendropdown.classList.remove('show');
            }
        }
    }
}

function changePassword() {
    document.getElementById('formback').submit();
}
