function sendAnotherUserForm(element) {
    const authorLogin = element.getAttribute('data-login');
    const form = document.createElement('form');
    form.method = 'GET';
    form.action = '/user/anotherUserPage';

    const input = document.createElement('input');
    input.type = 'hidden';
    input.name = 'anotherUserLogin';
    input.value = authorLogin;

    form.appendChild(input);
    document.body.appendChild(form);
    form.submit();
}