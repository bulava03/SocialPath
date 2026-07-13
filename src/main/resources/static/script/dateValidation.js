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

/**
 * Reads the day/month/year fields and fills the hidden dateOfBirth input.
 * The date of birth is optional: all three fields empty is fine, but a
 * partially filled date is rejected. Returns true when the form may be
 * submitted.
 */
function prepareDateOfBirth() {
    const dayRaw = document.getElementById("dob-day").value.trim();
    const monthRaw = document.getElementById("dob-month").value.trim();
    const yearRaw = document.getElementById("dob-year").value.trim();
    const errorText = document.getElementById("errortext");
    const dateOfBirthInput = document.getElementById("dateOfBirth");

    if (dayRaw === "" && monthRaw === "" && yearRaw === "") {
        dateOfBirthInput.value = "";
        return true;
    }

    if (dayRaw === "" || monthRaw === "" || yearRaw === "") {
        errorText.innerText = "Enter the full date of birth or leave all three fields empty.";
        return false;
    }

    if (!isValidNumber(dayRaw) || !isValidNumber(monthRaw) || !isValidNumber(yearRaw)) {
        errorText.innerText = "Invalid date entered";
        return false;
    }

    const day = parseInt(dayRaw, 10);
    const month = parseInt(monthRaw, 10);
    const year = parseInt(yearRaw, 10);

    if (!validateDateOfBirth(day, month, year)) {
        errorText.innerText = "Invalid date entered";
        return false;
    }

    const composed = new Date(Date.UTC(year, month - 1, day));
    if (composed.getTime() > Date.now()) {
        errorText.innerText = "Date of birth must be in the past.";
        return false;
    }

    dateOfBirthInput.value = composed.toISOString();
    return true;
}

function registerUser() {
    event.preventDefault();

    if (prepareDateOfBirth()) {
        document.forms["regform"].submit();
    }
}
