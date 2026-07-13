function authorize() {
    let login = document.getElementById('login').value;
    let password = document.getElementById('password').value;

    let formData = new FormData();
    formData.append('login', login);
    formData.append('password', password);

    fetch('/auth/login', {
        method: 'POST',
        body: formData
    })
    .then(response => {
        if (response.ok) {
            // The server has set the HttpOnly auth cookie; just navigate.
            document.getElementById('authForm').submit();
        } else {
            document.getElementById('errortext').innerText = "Incorrect login or password";
        }
    })
    .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
        document.getElementById('errortext').innerText = "Incorrect login or password";
    });
}
