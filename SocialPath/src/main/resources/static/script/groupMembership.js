function getFormData() {
    return {
        groupId: document.getElementById("formGroupId").value,
        authorLogin: document.getElementById("formAuthorLogin").value,
        authorPassword: document.getElementById("formAuthorPassword").value
    };
}

function handleJoinResponse(responseCode) {
    if (responseCode === -1) {
        alert("Групу не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        alert("Ви вже належите до цієї групи. Перезавантажте сторінку.");
    } else if (responseCode === 1) {
        const button = document.getElementById("joinButton");
        button.id = "leaveButton";
        button.setAttribute("onclick", "leaveGroup()");
        button.textContent = "Покинути групу";
    }
}

function handleLeaveResponse(responseCode) {
    if (responseCode === -1) {
        alert("Групу не знайдено або трапилась помилка авторизації.");
    } else if (responseCode === 0) {
        alert("Ви не належите до цієї групи. Перезавантажте сторінку.");
    } else if (responseCode === 1) {
        const button = document.getElementById("leaveButton");
        button.id = "joinButton";
        button.setAttribute("onclick", "joinGroup()");
        button.textContent = "Долучитися до групи";
    }
}

function sendPostRequest(url, data, handleResponseFunction) {
    const formData = new FormData();
    formData.append("groupId", data.groupId);
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

function joinGroup() {
    const data = getFormData();
    sendPostRequest("/groupMembership/joinGroup", data, handleJoinResponse);
}

function leaveGroup() {
    const data = getFormData();
    sendPostRequest("/groupMembership/leaveGroup", data, handleLeaveResponse);
}
