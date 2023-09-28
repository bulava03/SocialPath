function updateAdminConfirm(login, password) {
    document.getElementById("loginUpdate").value = login;
    document.getElementById("passwordUpdate").value = password;
    document.getElementById("updateForm").submit();
}

function removeAdminConfirm(login, password) {
    document.getElementById("loginRemove").value = login;
    document.getElementById("passwordRemove").value = password;
    document.getElementById("removeForm").submit();
}
