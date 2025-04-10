function loadComments(login) {
    const url = new URL("/commentsOld/loadCommentsUser", window.location.origin);
    url.searchParams.append("login", login);

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

function submitPublication(login) {
    const textArea = document.getElementById("comment-content");

    const formData = new FormData();
    formData.append("text", textArea.value);

    textArea.value = '';

    fetch("/comments/addUserPublication", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(login);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitComment(parentId, pageLogin) {
    const textArea = document.getElementsByClassName('reply-field ' + parentId)[0];

    const formData = new FormData();
    formData.append("idPublication", parentId);
    formData.append("text", textArea.value);

    fetch("/comments/addCommentUser", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(pageLogin);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitRemoveComment(commentId, pageLogin) {
    let comment = document.getElementById(commentId);
    let publication = comment.parentNode;

    const formData = new FormData();
    formData.append("idPublication", publication.id);
    formData.append("idComment", comment.id);

    fetch("/comments/removeCommentUser", {
        method: "POST",
        body: formData,
    })
    .then(response => response.json())
        .then(data => {
            if (data === -1) {
                console.log('error');
            } else if (data === 0) {
                loadComments(pageLogin);
            }
        })
    .catch(error => {
        console.error("Request failed:", error);
    });
}
