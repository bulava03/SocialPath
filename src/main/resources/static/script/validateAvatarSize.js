function validateAvatarSize() {
    event.preventDefault();

    const fileInput = document.getElementById('file');
    const file = fileInput.files[0];
    const maxFileSize = 50 * 1024 * 1024; // 50 MB

    if (file && file.size > maxFileSize) {
        alert('The photo size must not exceed 50 MB');
        return;
    }

    if (prepareDateOfBirth()) {
        document.forms["regform"].submit();
    }
}
