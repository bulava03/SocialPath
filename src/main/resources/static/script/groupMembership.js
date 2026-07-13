function getGroupId() {
    return document.getElementById("formGroupId").value;
}

function handleJoinResponse(data) {
    if (data.result === "ALREADY_MEMBER") {
        alert("You are already a member of this group. Reload the page.");
    } else if (data.result === "JOINED") {
        const button = document.getElementById("joinButton");
        button.id = "leaveButton";
        button.setAttribute("onclick", "leaveGroup()");
        button.textContent = "Leave group";
    }
}

function handleLeaveResponse(data) {
    if (data.result === "NOT_A_MEMBER") {
        alert("You are not a member of this group. Reload the page.");
    } else if (data.result === "LEFT") {
        const button = document.getElementById("leaveButton");
        button.id = "joinButton";
        button.setAttribute("onclick", "joinGroup()");
        button.textContent = "Join group";
    }
}

function sendPostRequest(url, groupId, handleResponseFunction) {
    const formData = new FormData();
    formData.append("groupId", groupId);

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

function joinGroup() {
    sendPostRequest("/groupMembership/joinGroup", getGroupId(), handleJoinResponse);
}

function leaveGroup() {
    sendPostRequest("/groupMembership/leaveGroup", getGroupId(), handleLeaveResponse);
}
