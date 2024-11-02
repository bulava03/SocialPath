function getFormData() {
    return {
        login: document.getElementById("userLogin").value,
        authorLogin: document.getElementById("authorLogin").value,
        authorPassword: document.getElementById("authorPassword").value
    };
}

function handleInviteResponse(responseCode) {
    if (responseCode === -1) {
        alert("Користувача не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        alert("Користувач вже в друзях. Перезавантажте сторінку.");
    } else if (responseCode === 1) {
        alert("Запит на дружбу вже існує. Перезавантажте сторінку.");
    } else if (responseCode === 2) {
        const button = document.getElementById("submitInvite");
        button.id = "submitRemoveInvite";
        button.setAttribute("onclick", "removeInviteToFriends()");
        button.textContent = "Скасувати запит на дружбу";
    }
}

function handleRemoveInviteResponse(responseCode) {
    if (responseCode === -1) {
        alert("Користувача не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0 || responseCode === 1) {
        const button = document.getElementById("submitRemoveInvite");
        button.id = "submitInvite";
        button.setAttribute("onclick", "inviteToFriends()");
        button.textContent = "Запросити в друзі";
    }
}

function handleRejectResponse(responseCode) {
    if (responseCode === -1) {
        alert("Користувача не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        const rejectButton = document.getElementById("submitReject");
        rejectButton.id = "submitInvite";
        rejectButton.setAttribute("onclick", "inviteToFriends()");
        rejectButton.textContent = "Запросити в друзі";

        const acceptButton = document.getElementById("submitAccept");
        if (acceptButton) {
            acceptButton.remove();
        }
    }
}

function handleAcceptResponse(responseCode) {
    if (responseCode === -1) {
        alert("Користувача не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        alert("Користувач вже в друзях. Перезавантажте сторінку.");
    } else if (responseCode === 1) {
        const acceptButton = document.getElementById("submitAccept");
        acceptButton.id = "submitRemove";
        acceptButton.setAttribute("onclick", "removeFromFriends()");
        acceptButton.textContent = "Видалити з друзів";

        const rejectButton = document.getElementById("submitReject");
        if (rejectButton) {
            rejectButton.remove();
        }
    }
}

function handleRemoveResponse(responseCode) {
    if (responseCode === -1) {
        alert("Користувача не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        const removeButton = document.getElementById("submitRemove");
        removeButton.id = "submitInvite";
        removeButton.setAttribute("onclick", "inviteToFriends()");
        removeButton.textContent = "Запросити в друзі";
    }
}

function sendPostRequest(url, data, handleResponseFunction) {
    const formData = new FormData();
    formData.append("login", data.login);
    formData.append("authorLogin", data.authorLogin);
    formData.append("authorPassword", data.authorPassword);

    fetch(url, {
        method: "POST",
        body: formData
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else {
            console.log("Помилка сервера: " + response.status);
            return null;
        }
    })
    .then(responseCode => {
        if (responseCode !== null) {
            handleResponseFunction(responseCode);
        }
    })
    .catch(error => {
        console.log("Помилка з'єднання:", error);
    });
}

function inviteToFriends() {
    const data = getFormData();
    sendPostRequest("/friends/inviteToFriends", data, handleInviteResponse);
}

function removeInviteToFriends() {
    const data = getFormData();
    sendPostRequest("/friends/removeInviteToFriends", data, handleRemoveInviteResponse);
}

function rejectInvitationToFriends() {
    const data = getFormData();
    sendPostRequest("/friends/rejectInvitationToFriends", data, handleRejectResponse);
}

function acceptToFriends() {
    const data = getFormData();
    sendPostRequest("/friends/acceptToFriends", data, handleAcceptResponse);
}

function removeFromFriends() {
    const data = getFormData();
    sendPostRequest("/friends/removeFromFriends", data, handleRemoveResponse);
}
