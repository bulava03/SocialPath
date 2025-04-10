function loadComments(groupId) {
    const url = new URL("/commentsOld/loadCommentsGroup", window.location.origin);
    url.searchParams.append("groupId", groupId);

    fetch(url, {
        method: "GET",
    })
    .then(response => response.text())
    .then(data => {
        if (data !== "") {
            document.getElementById("publications").innerHTML = data;
        } else {
            console.log("No comments available.");
        }
    })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitPublication(groupId) {
    const textArea = document.getElementById("comment-content");

    const formData = new FormData();
    formData.append("text", textArea.value);
    formData.append("groupId", groupId);

    textArea.value = '';

    fetch("/comments/addGroupPublication", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(groupId);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitComment(parentId, groupId) {
    const textArea = document.getElementsByClassName('reply-field ' + parentId)[0];

    const formData = new FormData();
    formData.append("idPublication", parentId);
    formData.append("groupId", groupId);
    formData.append("text", textArea.value);

    fetch("/comments/addCommentGroup", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(groupId);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitRemoveComment(commentId, groupId) {
    let comment = document.getElementById(commentId);
    let publication = comment.parentNode;

    const formData = new FormData();
    formData.append("idPublication", publication.id);
    formData.append("groupId", groupId);
    formData.append("idComment", comment.id);

    fetch("/comments/removeCommentGroup", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(groupId);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}
