document.getElementById('publications').addEventListener('click', function(event) {
    if (event.target && event.target.id.startsWith('publication-add-media-btn')) {
        const idSuffix = event.target.id.replace('publication-add-media-btn', '');
        document.getElementById('publication-media' + idSuffix).click();
    }
});

document.getElementById('publications').addEventListener('change', function(event) {
    if (event.target && event.target.id.startsWith('publication-media')) {
        const idSuffix = event.target.id.replace('publication-media', '');
        const previewContainer = document.getElementById('publication-media-preview' + idSuffix);
        previewContainer.innerHTML = '';

        const files = event.target.files;
        if (files.length > 5) {
            alert('Максимум 5 фото або 1 відео.');
            event.target.value = '';
            return;
        }

        let hasVideo = false;
        for (let file of files) {
            if (file.type.startsWith('video/')) {
                hasVideo = true;
                if (files.length > 1) {
                    alert('Можна тільки одне відео без фото.');
                    event.target.value = '';
                    return;
                }
            }
        }

        for (let file of files) {
            const fileURL = URL.createObjectURL(file);
            let mediaElement;
            if (file.type.startsWith('image/')) {
                mediaElement = document.createElement('img');
                mediaElement.src = fileURL;
                mediaElement.classList.add('preview-image');
            } else if (file.type.startsWith('video/')) {
                mediaElement = document.createElement('video');
                mediaElement.src = fileURL;
                mediaElement.controls = true;
                mediaElement.classList.add('preview-video');
            }
            previewContainer.appendChild(mediaElement);
        }
    }
});

// Обробник для публікацій без суфікса
document.querySelectorAll('#publication-add-media-btn').forEach(button => {
    button.addEventListener('click', function(event) {
        const mediaInput = document.getElementById('publication-media');
        if (mediaInput) {
            mediaInput.click();
        }
    });
});

document.querySelectorAll('#publication-media').forEach(input => {
    input.addEventListener('change', function(event) {
        const previewContainer = document.getElementById('publication-media-preview' + (event.target.id.replace('publication-media', '') || ''));
        previewContainer.innerHTML = '';

        const files = event.target.files;
        if (files.length > 5) {
            alert('Максимум 5 фото або 1 відео.');
            event.target.value = '';
            return;
        }

        let hasVideo = false;
        for (let file of files) {
            if (file.type.startsWith('video/')) {
                hasVideo = true;
                if (files.length > 1) {
                    alert('Можна тільки одне відео без фото.');
                    event.target.value = '';
                    return;
                }
            }
        }

        for (let file of files) {
            const fileURL = URL.createObjectURL(file);
            let mediaElement;
            if (file.type.startsWith('image/')) {
                mediaElement = document.createElement('img');
                mediaElement.src = fileURL;
                mediaElement.classList.add('preview-image');
            } else if (file.type.startsWith('video/')) {
                mediaElement = document.createElement('video');
                mediaElement.src = fileURL;
                mediaElement.controls = true;
                mediaElement.classList.add('preview-video');
            }
            previewContainer.appendChild(mediaElement);
        }
    });
});
