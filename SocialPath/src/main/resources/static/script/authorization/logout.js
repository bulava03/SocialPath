function logout() {
  fetch('/auth/logout', {
    method: 'POST',
    headers: {
      'Authorization': `Bearer ${localStorage.getItem('jwtToken')}`
    }
  })
  .then(response => {
    if (response.ok) {
      document.cookie = "token=; path=/; expires=Thu, 01 Jan 1970 00:00:00 GMT;";
      window.location.href = '/';
    } else {
      console.error('Помилка при логауті:', response.status);
    }
  })
  .catch(error => {
    console.error('Помилка при виконанні fetch:', error);
  });
}
