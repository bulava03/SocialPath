// Posts and comments on a user wall or a group wall. The page tells which one
// through data attributes on #publications:
//   data-scope="user" | "group", data-target=<user login | group id>.
// Permissions are enforced by the server; these attributes only pick the
// endpoints the page talks to.

const SCOPES = {
    user: {
        load: "/comments/view/loadCommentsUser",
        latest: "/comments/view/latestUser",
        targetParam: "login",
        publish: "/comments/addUserPublication",
        comment: "/comments/addCommentUser",
        remove: "/comments/removeCommentUser",
        sendsTarget: false,
    },
    group: {
        load: "/comments/view/loadCommentsGroup",
        latest: "/comments/view/latestGroup",
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
 * Posts a form to the server. Resolves to the JSON answer on success; on
 * failure shows the server's message (or opens the login page on 401) and
 * resolves to null.
 */
function send(url, formData) {
    return fetch(url, { method: "POST", body: formData })
        .then(response => {
            if (response.status === 401) {
                window.location.href = "/";
                return null;
            }
            if (response.ok) {
                return response.json();
            }
            if (response.status === 413) {
                alert("The files are too large to upload.");
                return null;
            }
            return response.json()
                .then(err => alert(err.error || "Request failed."))
                .catch(() => alert("Request failed."))
                .then(() => null);
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("Could not reach the server. Check your connection and try again.");
            return null;
        });
}

/**
 * Loads a piece of the feed as HTML. Resolves to null when the session has
 * ended or the page is gone: fetch follows such redirects silently and would
 * hand back a whole other page, so the browser is reloaded instead.
 */
function fetchHtml(url) {
    return fetch(url).then(response => {
        if (response.redirected) {
            window.location.reload();
            return null;
        }
        if (!response.ok) {
            throw new Error("HTTP " + response.status);
        }
        return response.text();
    });
}

/** The next feed portion below a post id, or the first portion without one. */
function feedUrl(before) {
    const { config, target } = currentScope();
    const url = new URL(config.load, window.location.origin);
    url.searchParams.append(config.targetParam, target);
    if (before) {
        url.searchParams.append("before", before);
    }
    return url;
}

function threadUrl(id) {
    const url = new URL("/comments/view/thread", window.location.origin);
    url.searchParams.append("id", id);
    return url;
}

/** The top-level post an element belongs to. */
function postOf(element) {
    return element.closest("#publications > .comment");
}

/** Re-renders one post with its whole comment tree, in place. */
function refreshPost(post) {
    fetchHtml(threadUrl(post.id))
        .then(html => {
            if (html) {
                post.outerHTML = html;
            }
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("Could not refresh the post. Reload the page.");
        });
}

/** Puts a newly published post on top of the feed. */
function prependPost(id) {
    fetchHtml(threadUrl(id))
        .then(html => {
            if (html) {
                document.querySelector("#publications > .feed-empty")?.remove();
                document.getElementById("publications").insertAdjacentHTML("afterbegin", html);
            }
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("The post is published, but could not be shown. Reload the page.");
        });
}

function removePost(post) {
    post.remove();
    const publications = document.getElementById("publications");
    if (!publications.querySelector(":scope > .comment, :scope > .feed-more")) {
        publications.insertAdjacentHTML("beforeend", '<p class="feed-empty">No posts yet.</p>');
    }
}

// Infinite scroll. A marker after the last post carries the id to continue
// from; when it comes within two screens of the viewport the next portion is
// appended with a new marker. The last portion has no marker, and then the
// observer is switched off for good.
let feedObserver = null;
let feedLoading = false;

function loadMorePosts(marker) {
    if (feedLoading) {
        return;
    }
    feedLoading = true;
    feedObserver.unobserve(marker);
    marker.textContent = "Loading…";

    fetchHtml(feedUrl(marker.dataset.before))
        .then(html => {
            if (html === null) {
                return;
            }
            marker.insertAdjacentHTML("beforebegin", html);
            marker.remove();
            watchFeedEnd();
        })
        .catch(error => {
            console.error("Request failed:", error);
            marker.textContent = "Could not load more posts. ";
            const retry = document.createElement("button");
            retry.type = "button";
            retry.textContent = "Retry";
            retry.addEventListener("click", () => loadMorePosts(marker));
            marker.appendChild(retry);
        })
        .finally(() => {
            feedLoading = false;
        });
}

function watchFeedEnd() {
    const marker = document.querySelector("#publications > .feed-more");
    if (!marker) {
        feedObserver?.disconnect();
        feedObserver = null;
        return;
    }
    if (!feedObserver) {
        feedObserver = new IntersectionObserver(entries => {
            for (const entry of entries) {
                if (entry.isIntersecting) {
                    loadMorePosts(entry.target);
                }
            }
        }, { rootMargin: "0px 0px 200% 0px" });
    }
    feedObserver.observe(marker);
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

    send(config.publish, formData).then(result => {
        if (result) {
            clearPostForm("");
            prependPost(result.id);
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

    // Only this post is re-rendered (its reply form comes back empty); drafts
    // in other posts stay as they are.
    const post = postOf(document.getElementById(parentId));
    send(config.comment, formData).then(result => {
        if (result) {
            refreshPost(post);
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

    const comment = document.getElementById(commentId);
    const post = postOf(comment);
    send(config.remove, formData).then(result => {
        if (!result) {
            return;
        }
        if (comment === post) {
            removePost(post);
        } else {
            refreshPost(post);
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

// New activity by others. The page carries the newest post or comment id by
// someone else (data-latest); every 30 seconds while the tab is visible, and
// when it becomes visible again, the server is asked for the current one.
// At the top of the feed with nothing typed, the first portion is reloaded
// on the spot; anywhere else a banner offers it instead, so content never
// shifts under the reader. Reaching the top later applies it as well.
const ACTIVITY_CHECK_MS = 30_000;
let pendingLatest = null;

function shownLatest() {
    return Number(document.getElementById("publications").dataset.latest || 0);
}

function atFeedTop() {
    return document.getElementById("publications").getBoundingClientRect().top >= 0;
}

function hasDrafts() {
    const fields = document.querySelectorAll("#publication-text, .reply-field");
    const files = document.querySelectorAll("input[type='file'][id^='publication-media']");
    return [...fields].some(field => field.value.trim() !== "")
        || [...files].some(input => input.files.length > 0);
}

function checkActivity() {
    if (document.hidden) {
        return;
    }
    const { config, target } = currentScope();
    const url = new URL(config.latest, window.location.origin);
    url.searchParams.append(config.targetParam, target);

    fetch(url)
        .then(response => (response.ok && !response.redirected ? response.json() : null))
        .then(data => {
            if (data && data.latest > shownLatest()) {
                pendingLatest = data.latest;
                applyActivity();
            }
        })
        .catch(error => console.error("Request failed:", error));
}

function applyActivity() {
    if (pendingLatest === null) {
        return;
    }
    if (atFeedTop() && !hasDrafts()) {
        reloadFeed();
    } else {
        showNewsBanner();
    }
}

function showNewsBanner() {
    if (document.querySelector(".feed-news")) {
        return;
    }
    const banner = document.createElement("div");
    banner.className = "feed-news";
    banner.setAttribute("role", "status");
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = "New posts and comments · Refresh";
    button.addEventListener("click", () => {
        reloadFeed();
        document.getElementById("publications").scrollIntoView();
    });
    banner.appendChild(button);
    document.getElementById("publications").before(banner);
}

/** Replaces the feed with its first portion, as if the page had just opened. */
function reloadFeed() {
    const latest = pendingLatest;
    fetchHtml(feedUrl(null))
        .then(html => {
            if (html === null) {
                return;
            }
            const publications = document.getElementById("publications");
            publications.innerHTML = html;
            if (!publications.querySelector(":scope > .comment")) {
                publications.innerHTML = '<p class="feed-empty">No posts yet.</p>';
            }
            if (latest !== null) {
                publications.dataset.latest = Math.max(latest, shownLatest());
            }
            pendingLatest = null;
            document.querySelector(".feed-news")?.remove();
            feedObserver?.disconnect();
            feedObserver = null;
            watchFeedEnd();
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("Could not refresh the feed. Reload the page.");
        });
}

let scrollCheckQueued = false;
window.addEventListener("scroll", () => {
    if (pendingLatest === null || scrollCheckQueued) {
        return;
    }
    scrollCheckQueued = true;
    requestAnimationFrame(() => {
        scrollCheckQueued = false;
        if (atFeedTop() && !hasDrafts()) {
            applyActivity();
        }
    });
});
document.addEventListener("visibilitychange", checkActivity);
setInterval(checkActivity, ACTIVITY_CHECK_MS);

watchFeedEnd();

// Delegated on document: posts are replaced when they are refreshed, and the
// new-post form lives outside the feed.
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
