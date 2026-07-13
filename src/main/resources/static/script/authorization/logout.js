function logout() {
  fetch('/auth/logout', {
    method: 'POST'
  })
  .then(response => {
    if (response.ok) {
      window.location.href = '/';
    } else {
      console.error('Logout failed:', response.status);
    }
  })
  .catch(error => {
    console.error('Logout request failed:', error);
  });
}
