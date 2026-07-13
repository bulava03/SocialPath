function loadComments(login) {
    const url = new URL("/comments/view/loadCommentsUser", window.location.origin);
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
    const textArea = document.getElementById('publication-text');
    const text = textArea.value;
    const mediaInput = document.getElementById('publication-media');
    const files = mediaInput.files;

    if (!text && files.length === 0) {
        alert('Write something or attach media!');
        return;
    }

    // Validate media
    if (files.length > 0) {
        if (files.length > 5) {
            alert('Up to 5 photos or 1 video.');
            return;
        }

        let hasVideo = false;
        for (let file of files) {
            if (file.type.startsWith('video/')) {
                hasVideo = true;
                if (files.length > 1) {
                    alert('You can attach only a single video without photos.');
                    return;
                }
                if (file.size > 50 * 1024 * 1024) { // 50 MB
                    alert('A video must not exceed 50 MB.');
                    return;
                }
            } else if (file.type.startsWith('image/')) {
                if (file.size > 10 * 1024 * 1024) { // 10 MB
                    alert('A photo must not exceed 10 MB.');
                    return;
                }
            } else {
                alert('Only photos and mp4 videos are allowed.');
                return;
            }
        }
    }

    const formData = new FormData();
    formData.append("text", text);
    for (let file of files) {
        formData.append('media', file);
    }

    textArea.value = '';
    mediaInput.value = null;

    const mediaPreview = document.getElementById('publication-media-preview');
    while (mediaPreview.firstChild) {
        mediaPreview.removeChild(mediaPreview.firstChild);
    }

    fetch("/comments/addUserPublication", {
        method: "POST",
        body: formData,
    })
        .then(response => {
        if (response.status === 401) {
            window.location.href = "/";
        } else if (response.ok) {
            loadComments(login);
        } else {
            response.json()
                .then(err => alert(err.error || "Request failed."))
                .catch(() => alert("Request failed."));
        }
    })
    .catch(error => {
        console.error("Request failed:", error);
    });
}

function submitComment(parentId, pageLogin) {
    const textArea = document.getElementById('publication-text-' + parentId);
    const text = textArea.value;
    const mediaInput = document.getElementById('publication-media-' + parentId);
    const files = mediaInput.files;

    if (!text && files.length === 0) {
        alert('Write something or attach media!');
        return;
    }

    // Validate media
    if (files.length > 0) {
        if (files.length > 5) {
            alert('Up to 5 photos or 1 video.');
            return;
        }

        let hasVideo = false;
        for (let file of files) {
            if (file.type.startsWith('video/')) {
                hasVideo = true;
                if (files.length > 1) {
                    alert('You can attach only a single video without photos.');
                    return;
                }
                if (file.size > 50 * 1024 * 1024) { // 50 MB
                    alert('A video must not exceed 50 MB.');
                    return;
                }
            } else if (file.type.startsWith('image/')) {
                if (file.size > 10 * 1024 * 1024) { // 10 MB
                    alert('A photo must not exceed 10 MB.');
                    return;
                }
            } else {
                alert('Only photos and mp4 videos are allowed.');
                return;
            }
        }
    }

    const formData = new FormData();
    formData.append("idPublication", parentId);
    formData.append("text", text);
    for (let file of files) {
        formData.append('media', file);
    }

    fetch("/comments/addCommentUser", {
        method: "POST",
        body: formData,
    })
        .then(response => {
        if (response.status === 401) {
            window.location.href = "/";
        } else if (response.ok) {
            loadComments(pageLogin);
        } else {
            response.json()
                .then(err => alert(err.error || "Request failed."))
                .catch(() => alert("Request failed."));
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
        .then(response => {
        if (response.status === 401) {
            window.location.href = "/";
        } else if (response.ok) {
            loadComments(pageLogin);
        } else {
            response.json()
                .then(err => alert(err.error || "Request failed."))
                .catch(() => alert("Request failed."));
        }
    })
    .catch(error => {
        console.error("Request failed:", error);
    });
}
