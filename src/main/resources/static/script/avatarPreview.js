// Avatar picker widget: the round preview is clickable, "Change photo" opens
// the file dialog, "Remove" clears the current image (submitted via the
// hidden removeImage flag). Selecting a new file always cancels removal.
// Some photos (HEIC in Chrome/Opera) cannot be previewed by the browser; the
// server converts them on save, so the picker just says so.
(function () {
    const input = document.getElementById("file");
    const preview = document.getElementById("avatar-preview");
    if (!input || !preview) {
        return;
    }

    const removeFlag = document.getElementById("removeImage");
    const removeButton = document.getElementById("remove-photo");
    const note = document.getElementById("avatar-note");
    const defaultSrc = "/pictures/photo.png";
    const savedSrc = preview.getAttribute("src");

    function showNote(visible) {
        if (note) {
            note.hidden = !visible;
        }
    }

    function showingDefault() {
        return preview.getAttribute("src") === defaultSrc && !(input.files && input.files.length);
    }

    function refreshRemoveButton() {
        if (removeButton) {
            removeButton.style.display = showingDefault() ? "none" : "";
        }
    }

    preview.addEventListener("click", function () {
        input.click();
    });

    input.addEventListener("change", function () {
        const file = input.files && input.files[0];
        showNote(false);
        if (file) {
            const url = URL.createObjectURL(file);
            preview.onload = () => URL.revokeObjectURL(url);
            preview.onerror = () => {
                URL.revokeObjectURL(url);
                preview.src = savedSrc;
                showNote(true);
            };
            preview.src = url;
            if (removeFlag) {
                removeFlag.value = "false";
            }
        }
        refreshRemoveButton();
    });

    if (removeButton) {
        removeButton.addEventListener("click", function () {
            input.value = "";
            showNote(false);
            preview.src = defaultSrc;
            if (removeFlag) {
                removeFlag.value = "true";
            }
            refreshRemoveButton();
        });
    }

    refreshRemoveButton();
})();
