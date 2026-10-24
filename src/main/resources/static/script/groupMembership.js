// Join/leave buttons on a group page. Needs postAction.js.

function groupAction(url, handleResponse) {
    const groupId = document.getElementById("formGroupId").value;
    postAction(url, { groupId }).then(data => {
        if (data !== null) {
            handleResponse(data);
        }
    });
}

function handleJoinResponse(data) {
    if (data.result === "ALREADY_MEMBER") {
        alert("You are already a member of this group. Reload the page.");
    } else if (data.result === "JOINED") {
        swapButton("joinButton", "leaveButton", "leaveGroup()", "Leave group");
    }
}

function handleLeaveResponse(data) {
    if (data.result === "NOT_A_MEMBER") {
        alert("You are not a member of this group. Reload the page.");
    } else if (data.result === "LEFT") {
        swapButton("leaveButton", "joinButton", "joinGroup()", "Join group");
    }
}

function joinGroup() {
    groupAction("/groupMembership/joinGroup", handleJoinResponse);
}

function leaveGroup() {
    groupAction("/groupMembership/leaveGroup", handleLeaveResponse);
}
