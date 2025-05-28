document.addEventListener('DOMContentLoaded', () => {
  const container = document.querySelector('.jobs-section');
  const addButton = container.querySelector('.add-job-button');

  addButton.addEventListener('click', () => {
    const newInput = document.createElement('input');
    newInput.className = 'field jobs-field';
    newInput.name = 'jobs';
    newInput.type = 'text';
    container.insertBefore(newInput, addButton);
    removeEmptyInputs();
  });

  function removeEmptyInputs() {
    const inputs = Array.from(container.querySelectorAll('input.jobs-field'));
    for (let i = 0; i < inputs.length - 1; i++) {
      if (inputs[i].value.trim() === '') {
        container.removeChild(inputs[i]);
      }
    }
  }
});
