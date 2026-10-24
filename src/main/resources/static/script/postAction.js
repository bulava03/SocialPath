// Shared by the friend and group membership buttons: both post a single form
// field to a JSON endpoint and swap the button according to the result.

/**
 * Posts the given fields as a form. Resolves to the parsed JSON response, or
 * to null when there is nothing to act on: the problem has been shown to the
 * user, or the login page is being opened after a 401.
 */
function postAction(url, fields) {
    const formData = new FormData();
    for (const [name, value] of Object.entries(fields)) {
        formData.append(name, value);
    }

    return fetch(url, { method: "POST", body: formData })
        .then(response => {
            if (response.status === 401) {
                window.location.href = "/";
                return null;
            }
            if (!response.ok) {
                return response.json()
                    .then(err => alert(err.error || "Request failed."))
                    .catch(() => alert("Request failed."))
                    .then(() => null);
            }
            return response.json();
        })
        .catch(error => {
            console.error("Request failed:", error);
            alert("Could not reach the server. Check your connection and try again.");
            return null;
        });
}

/** Turns a button into a different action: new id, click handler and label. */
function swapButton(id, newId, onclick, text) {
    const button = document.getElementById(id);
    button.id = newId;
    button.setAttribute("onclick", onclick);
    button.textContent = text;
}
