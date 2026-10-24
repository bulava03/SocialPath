// Posts and comments on a user wall or a group wall. The page tells which one
// through data attributes on #publications:
//   data-scope="user" | "group", data-target=<user login | group id>.
// Permissions are enforced by the server; these attributes only pick the
// endpoints the page talks to.

const SCOPES = {
    user: {
        load: "/comments/view/loadCommentsUser",
        targetParam: "login",
        publish: "/comments/addUserPublication",
        comment: "/comments/addCommentUser",
        remove: "/comments/removeCommentUser",
        sendsTarget: false,
    },
    group: {
        load: "/comments/view/loadCommentsGroup",
        targetParam: "groupId",
        publish: "/comments/addGroupPublication",
        comment: "/comments/addCommentGroup",
        remove: "/comments/removeCommentGroup",
        sendsTarget: true,
    },
};

// Must match spring.servlet.multipart limits in application.properties.
const MAX_FILES = 5;
const MAX_PHOTO_BYTES = 10 * 1024 * 1024;
const MAX_VIDEO_BYTES = 100 * 1024 * 1024;

function currentScope() {
    const publications = document.getElementById("publications");
    return {
        config: SCOPES[publications.dataset.scope],
        target: publications.dataset.target,
    };
}

/**
 * Checks the selected files against the upload rules.
 * @returns {string|null} an error message, or null when the files are fine
 */
function validateMedia(files) {
    if (files.length > MAX_FILES) {
        return "Up to 5 photos or 1 video.";
    }
    for (const file of files) {
        if (file.type === "video/mp4") {
            if (files.length > 1) {
                return "You can attach only a single video without photos.";
            }
            if (file.size > MAX_VIDEO_BYTES) {
                return "A video must not exceed 100 MB.";
            }
        } else if (file.type.startsWith("image/")) {
            if (file.size > MAX_PHOTO_BYTES) {
                return "A photo must not exceed 10 MB.";
            }
        } else {
            return "Only photos and mp4 videos are allowed.";
        }
    }
    return null;
}

/**
 * Posts a form to the server. Resolves to true on success; on failure shows
 * the server's message (or sends the user to the login page on 401).
 */
function send(url, formData) {
    return fetch(url, { method: "POST", body: formData })
        .then(response => {
            if (response.status === 401) {
                window.location.href = "/";
                return false;
            }
            if (response.ok) {
                return true;
            }
            return response.json()
                .then(err => alert(err.error || "Request failed."))
                .catch(() => alert("Request failed."))
                .then(() => false);
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("Could not reach the server. Check your connection and try again.");
            return false;
        });
}

/** Re-renders the whole publications block with fresh data from the server. */
function loadComments() {
    const { config, target } = currentScope();
    const url = new URL(config.load, window.location.origin);
    url.searchParams.append(config.targetParam, target);

    fetch(url)
        .then(response => response.text())
        .then(html => {
            if (html !== "") {
                // The fragment includes the #publications element itself.
                document.getElementById("publications").outerHTML = html;
            }
        })
        .catch(error => console.error("Request failed:", error));
}

/**
 * Builds the request for a new post or comment from the text field and file
 * input with the given id suffix. Returns null (after telling the user) when
 * there is nothing to send or the media is invalid.
 */
function buildPostForm(idSuffix) {
    const text = document.getElementById("publication-text" + idSuffix).value;
    const files = document.getElementById("publication-media" + idSuffix).files;

    if (!text && files.length === 0) {
        alert("Write something or attach media!");
        return null;
    }
    const mediaError = validateMedia(files);
    if (mediaError) {
        alert(mediaError);
        return null;
    }

    const formData = new FormData();
    formData.append("text", text);
    for (const file of files) {
        formData.append("media", file);
    }
    return formData;
}

function clearPostForm(idSuffix) {
    document.getElementById("publication-text" + idSuffix).value = "";
    document.getElementById("publication-media" + idSuffix).value = "";
    document.getElementById("publication-media-preview" + idSuffix).replaceChildren();
}

function submitPublication() {
    const { config, target } = currentScope();
    const formData = buildPostForm("");
    if (!formData) {
        return;
    }
    if (config.sendsTarget) {
        formData.append(config.targetParam, target);
    }

    send(config.publish, formData).then(ok => {
        if (ok) {
            clearPostForm("");
            loadComments();
        }
    });
}

function submitComment(parentId) {
    const { config, target } = currentScope();
    const formData = buildPostForm("-" + parentId);
    if (!formData) {
        return;
    }
    formData.append("idPublication", parentId);
    if (config.sendsTarget) {
        formData.append(config.targetParam, target);
    }

    // The reply form is re-rendered empty by loadComments().
    send(config.comment, formData).then(ok => {
        if (ok) {
            loadComments();
        }
    });
}

function submitRemoveComment(commentId) {
    const { config, target } = currentScope();
    const formData = new FormData();
    formData.append("idComment", commentId);
    if (config.sendsTarget) {
        formData.append(config.targetParam, target);
    }

    send(config.remove, formData).then(ok => {
        if (ok) {
            loadComments();
        }
    });
}

/** Hides every reply form the user has not typed anything into. */
function hideEmptyReplySections(exceptSection) {
    for (const section of document.querySelectorAll(".reply-section")) {
        if (section === exceptSection || section.querySelector(".reply-field").value !== "") {
            continue;
        }
        section.querySelector(".media-field").value = "";
        section.querySelector(".media-preview").replaceChildren();
        section.style.display = "none";
    }
}

/** Opens the reply form under a post or comment. */
function makeVisible(commentId) {
    hideEmptyReplySections(null);
    const section = document.querySelector(".reply-section." + CSS.escape(commentId));
    if (section) {
        section.style.display = "block";
    }
}

function renderPreview(input) {
    const idSuffix = input.id.replace("publication-media", "");
    const preview = document.getElementById("publication-media-preview" + idSuffix);
    preview.replaceChildren();

    const mediaError = validateMedia(input.files);
    if (mediaError) {
        alert(mediaError);
        input.value = "";
        return;
    }

    for (const file of input.files) {
        const url = URL.createObjectURL(file);
        let element;
        if (file.type.startsWith("image/")) {
            element = document.createElement("img");
            element.classList.add("preview-image");
        } else {
            element = document.createElement("video");
            element.controls = true;
            element.classList.add("preview-video");
        }
        element.src = url;
        preview.appendChild(element);
    }
}

// Delegated on document: #publications is replaced on every reload, and the
// new-post form lives outside it.
document.addEventListener("click", event => {
    const target = event.target;

    if (target.classList.contains("publication-add-media-btn")) {
        const idSuffix = target.id.replace("publication-add-media-btn", "");
        document.getElementById("publication-media" + idSuffix).click();
    }

    // A click outside a reply form closes it if it is still empty.
    if (!target.classList.contains("reply-btn")) {
        hideEmptyReplySections(target.closest(".reply-section"));
    }
});

document.addEventListener("change", event => {
    const target = event.target;
    if (target.type === "file" && target.id.startsWith("publication-media")) {
        renderPreview(target);
    }
});
