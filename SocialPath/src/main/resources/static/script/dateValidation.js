function validateDateOfBirth(day, month, year) {
    if (day < 1 || day > 31) {
        return false;
    }

    if (month < 1 || month > 12) {
        return false;
    }

    if (year < 1900 || year > new Date().getFullYear()) {
        return false;
    }

    if (day > new Date(year, month, 0).getDate()) {
        return false;
    }

    return true;
}

function isValidNumber(value) {
    return /^\d+$/.test(value);
}

function registerUser() {
    event.preventDefault();

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
