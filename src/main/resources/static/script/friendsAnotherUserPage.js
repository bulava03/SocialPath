// Friend request buttons on another user's page. Needs postAction.js.

function friendAction(url, handleResponse) {
    const login = document.getElementById("userLogin").value;
    postAction(url, { login }).then(data => {
        if (data !== null) {
            handleResponse(data);
        }
    });
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
        document.getElementById("submitAccept")?.remove();
    }
}

function handleAcceptResponse(data) {
    if (data.result === "ALREADY_FRIENDS") {
        alert("This user is already your friend. Reload the page.");
    } else if (data.result === "ACCEPTED") {
        swapButton("submitAccept", "submitRemove", "removeFromFriends()", "Remove from friends");
        document.getElementById("submitReject")?.remove();
    } else {
        alert("The friend request no longer exists. Reload the page.");
    }
}

function handleRemoveResponse(data) {
    if (data.result === "REMOVED_FROM_FRIENDS") {
        swapButton("submitRemove", "submitInvite", "inviteToFriends()", "Add friend");
    }
}

function inviteToFriends() {
    friendAction("/friends/inviteToFriends", handleInviteResponse);
}

function removeInviteToFriends() {
    friendAction("/friends/removeInviteToFriends", handleRemoveInviteResponse);
}

function rejectInvitationToFriends() {
    friendAction("/friends/rejectInvitationToFriends", handleRejectResponse);
}

function acceptToFriends() {
    friendAction("/friends/acceptToFriends", handleAcceptResponse);
}

function removeFromFriends() {
    friendAction("/friends/removeFromFriends", handleRemoveResponse);
}
