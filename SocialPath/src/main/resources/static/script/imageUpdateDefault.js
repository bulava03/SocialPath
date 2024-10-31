var input = document.getElementById('file');
const base64String = document.getElementById('imageToUploadByDefault').innerHTML;

if (base64String != "" && base64String != null) {
    const byteCharacters = atob(base64String);
    const byteNumbers = new Array(byteCharacters.length);
    for (let i = 0; i < byteCharacters.length; i++) {
        byteNumbers[i] = byteCharacters.charCodeAt(i);
    }
    const byteArray = new Uint8Array(byteNumbers);

    const file = new File([byteArray], "image.jpeg", {type:"image/jpeg"});

    var dt = new DataTransfer();
    dt.items.add(file);
    const file_list = dt.files;

    input.files = file_list;
}
