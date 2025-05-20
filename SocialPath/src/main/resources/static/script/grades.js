const overlay = document.getElementById('overlay');
const formContainer = document.getElementById('ratingFormContainer');
const providerIdInput = document.getElementById('providerId');
const stars = document.querySelectorAll('#stars .star');
const ratingForm = document.getElementById('ratingForm');
const ratingInput = document.getElementById('ratingInput');

let selectedRating = 0;
let hoverRating = 0;

function openRatingForm(providerId) {
  providerIdInput.value = providerId;
  selectedRating = 0;
  hoverRating = 0;
  updateStars(0);
  ratingForm.comment.value = '';
  overlay.style.display = 'block';
  formContainer.style.display = 'block';
}

function closeRatingForm() {
  overlay.style.display = 'none';
  formContainer.style.display = 'none';
}

// Оновлення кольорів зірок з урахуванням hover і selected
function updateStars(rating) {
  stars.forEach(star => {
    const val = Number(star.dataset.value);
    if (val <= rating) {
      star.classList.add('selected');
      star.style.color = 'gold';
      star.style.textShadow = '0 0 2px #b8860b';
    } else {
      star.classList.remove('selected');
      star.style.color = 'white';
      star.style.textShadow = '0 0 1px black';
    }
  });
}

function updateHoverStars(hoverVal) {
  stars.forEach(star => {
    const val = Number(star.dataset.value);
    if (val <= hoverVal) {
      // Світліше золото для hover
      star.style.color = '#ffd966'; // світло-золотий
      star.style.textShadow = '0 0 1px #aa8500';
    } else if (val <= selectedRating) {
      // Закріплені зірки — темніше золото
      star.style.color = 'gold';
      star.style.textShadow = '0 0 2px #b8860b';
    } else {
      // Порожні
      star.style.color = 'white';
      star.style.textShadow = '0 0 1px black';
    }
  });
}

stars.forEach(star => {
  star.addEventListener('click', () => {
    selectedRating = Number(star.dataset.value);
    hoverRating = 0;
    updateStars(selectedRating);
  });

  star.addEventListener('mouseenter', () => {
    hoverRating = Number(star.dataset.value);
    updateHoverStars(hoverRating);
  });

  star.addEventListener('click', () => {
    const selectedRating = Number(star.dataset.value);
    ratingInput.value = selectedRating;
    updateStars(selectedRating);
  });
});

formContainer.addEventListener('mouseleave', () => {
  hoverRating = 0;
  updateStars(selectedRating);
});

overlay.addEventListener('click', closeRatingForm);

function validateRating() {
  if (selectedRating === 0) {
    alert('Будь ласка, оберіть оцінку від 1 до 5 зірочок.');
    return false;
  }
  let ratingInput = ratingForm.querySelector('input[name="rating"]');
  if (!ratingInput) {
    ratingInput = document.createElement('input');
    ratingInput.type = 'hidden';
    ratingInput.name = 'rating';
    ratingForm.appendChild(ratingInput);
  }
  ratingInput.value = selectedRating;
  ratingForm.submit();
  return true;
}

window.openRatingForm = openRatingForm;
window.validateRating = validateRating;

// -------------- Відгуки --------------

const reviewsContainer = document.getElementById('reviewsContainer');

function openReviews() {
  document.getElementById('review-container').style.display = 'block';
  document.getElementById('overlay').style.display = 'block';
}

function closeReviews() {
  document.getElementById('review-container').style.display = 'none';
  document.getElementById('overlay').style.display = 'none';
}

// Розширення обробника overlay для обох вікон
overlay.addEventListener('click', () => {
  closeRatingForm();
  closeReviews();
});
