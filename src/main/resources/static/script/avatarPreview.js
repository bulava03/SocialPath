// Avatar picker widget: the round preview is clickable, "Change photo" opens
// the file dialog, "Remove" clears the current image (submitted via the
// hidden removeImage flag). Selecting a new file always cancels removal.
(function () {
    const input = document.getElementById("file");
    const preview = document.getElementById("avatar-preview");
    if (!input || !preview) {
        return;
    }

    const removeFlag = document.getElementById("removeImage");
    const removeButton = document.getElementById("remove-photo");
    const defaultSrc = "/pictures/photo.png";

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
        if (file && file.type.startsWith("image/")) {
            const url = URL.createObjectURL(file);
            preview.src = url;
            preview.onload = () => URL.revokeObjectURL(url);
            if (removeFlag) {
                removeFlag.value = "false";
            }
        }
        refreshRemoveButton();
    });

    if (removeButton) {
        removeButton.addEventListener("click", function () {
            input.value = "";
            preview.src = defaultSrc;
            if (removeFlag) {
                removeFlag.value = "true";
            }
            refreshRemoveButton();
        });
    }

    refreshRemoveButton();
})();
