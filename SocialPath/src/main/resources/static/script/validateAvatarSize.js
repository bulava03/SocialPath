function validateAvatarSize() {
    event.preventDefault();

    const fileInput = document.getElementById('file');
    const file = fileInput.files[0];
    const maxFileSize = 50 * 1024 * 1024; // 50 MB

    if (file && file.size > maxFileSize) {
        alert('Розмір фотографії не може перевищувати 50 МБ');
    } else {
        try {
            var day = parseInt(document.getElementById("dob-day").value);
            var month = parseInt(document.getElementById("dob-month").value);
            var year = parseInt(document.getElementById("dob-year").value);

            day = isValidNumber(day) ? parseInt(day) : NaN;
            month = isValidNumber(month) ? parseInt(month) : NaN;
            year = isValidNumber(year) ? parseInt(year) : NaN;

            if (!validateDateOfBirth(day, month, year) || isNaN(day) || isNaN(month) || isNaN(year)) {
                document.getElementById("errortext").innerHTML = "Введено некоректну дату";
                return;
            }
        } catch (ex) {
            document.getElementById("errortext").innerHTML = "Введено некоректну дату";
            return;
        }

        var dateOfBirth = new Date(Date.UTC(year, month - 1, day));
        var dateOfBirthInput = document.getElementById("dateOfBirth");
        dateOfBirthInput.value = dateOfBirth.toISOString();
        document.forms["regform"].submit();
    }
}
