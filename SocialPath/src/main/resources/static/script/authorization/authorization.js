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
        if (!response.ok) {
            throw new Error('Network response was not ok');
        }
        return response.text();
    })
    .then(token => {
        if (token !== "fail") {
            document.cookie = "token=" + token + "; path=/; secure; samesite=strict";

            let authForm = document.getElementById('authForm');
            authForm.submit();
        } else {
            document.getElementById('errortext').innerText = "Неправильний логін або пароль";
        }
    })
    .catch(error => {
        console.error('There was a problem with the fetch operation:', error);
        document.getElementById('errortext').innerText = "Неправильний логін або пароль";
    });
}
