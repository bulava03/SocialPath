function validateAvatarSize() {
    event.preventDefault();

    const fileInput = document.getElementById('file');
    const file = fileInput.files[0];
    const maxFileSize = 50 * 1024 * 1024; // 50 MB

    if (file && file.size > maxFileSize) {
        alert('Розмір фотографії не може перевищувати 50 МБ');
    } else {
        document.forms["regform"].submit();
    }
}
