function getFormData() {
    return {
        login: document.getElementById("userLogin").value
    };
}

function swapButton(id, newId, onclick, text) {
    const button = document.getElementById(id);
    button.id = newId;
    button.setAttribute("onclick", onclick);
    button.textContent = text;
}

function handleInviteResponse(data) {
    if (data.result === "ALREADY_FRIENDS") {
        alert("This user is already your friend. Reload the page.");
    } else if (data.result === "ALREADY_INVITED") {
        alert("A friend request already exists. Reload the page.");
    } else if (data.result === "INVITED") {
        swapButton("submitInvite", "submitRemoveInvite", "removeInviteToFriends()", "Cancel friend request");
    }
}

function handleRemoveInviteResponse(data) {
    if (data.result === "REMOVED_FROM_FRIENDS" || data.result === "INVITE_CANCELLED") {
        swapButton("submitRemoveInvite", "submitInvite", "inviteToFriends()", "Add friend");
    } else {
        alert("Nothing to cancel. Reload the page.");
    }
}

function handleRejectResponse(data) {
    if (data.result === "INVITE_REJECTED") {
        swapButton("submitReject", "submitInvite", "inviteToFriends()", "Add friend");

        const acceptButton = document.getElementById("submitAccept");
        if (acceptButton) {
            acceptButton.remove();
        }
    }
}

function handleAcceptResponse(data) {
    if (data.result === "ALREADY_FRIENDS") {
        alert("This user is already your friend. Reload the page.");
    } else if (data.result === "ACCEPTED") {
        swapButton("submitAccept", "submitRemove", "removeFromFriends()", "Remove from friends");

        const rejectButton = document.getElementById("submitReject");
        if (rejectButton) {
            rejectButton.remove();
        }
    } else {
        alert("The friend request no longer exists. Reload the page.");
    }
}

function handleRemoveResponse(data) {
    if (data.result === "REMOVED_FROM_FRIENDS") {
        swapButton("submitRemove", "submitInvite", "inviteToFriends()", "Add friend");
    }
}

function sendPostRequest(url, data, handleResponseFunction) {
    const formData = new FormData();
    formData.append("login", data.login);

    fetch(url, {
        method: "POST",
        body: formData
    })
    .then(response => {
        if (response.status === 401) {
            window.location.href = "/";
            return null;
        }
        if (!response.ok) {
            return response.json()
                .then(err => { alert(err.error || "Request failed."); return null; })
                .catch(() => { alert("Request failed."); return null; });
        }
        return response.json();
    })
    .then(data => {
        if (data !== null) {
            handleResponseFunction(data);
        }
    })
    .catch(error => {
        console.log("Connection error:", error);
    });
}

function inviteToFriends() {
    sendPostRequest("/friends/inviteToFriends", getFormData(), handleInviteResponse);
}

function removeInviteToFriends() {
    sendPostRequest("/friends/removeInviteToFriends", getFormData(), handleRemoveInviteResponse);
}

function rejectInvitationToFriends() {
    sendPostRequest("/friends/rejectInvitationToFriends", getFormData(), handleRejectResponse);
}

function acceptToFriends() {
    sendPostRequest("/friends/acceptToFriends", getFormData(), handleAcceptResponse);
}

function removeFromFriends() {
    sendPostRequest("/friends/removeFromFriends", getFormData(), handleRemoveResponse);
}
