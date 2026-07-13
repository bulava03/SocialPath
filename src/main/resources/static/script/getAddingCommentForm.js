function makeVisible(commentId) {
    var replySections = document.getElementsByClassName('reply-section');
    for (var i = 0; i < replySections.length; i++) {
        var replyField = replySections[i].getElementsByClassName('reply-field')[0];
        if (replyField.value === '') {
            replySections[i].style.display = 'none';
        }
    }

    var targetReplySection = document.getElementsByClassName('reply-section ' + commentId)[0];
    if (targetReplySection) {
        targetReplySection.style.display = 'block';
    }
}

document.addEventListener('click', function (event) {
    if (event.target.classList.contains('reply-btn')) {
        return;
    }

    var replySections = document.getElementsByClassName('reply-section');
    for (var i = 0; i < replySections.length; i++) {
        var replyField = replySections[i].getElementsByClassName('reply-field')[0];
        var mediaField = replySections[i].getElementsByClassName('media-field')[0];
        var mediaPreview = replySections[i].getElementsByClassName('media-preview')[0];
        if (
            (event.target === replySections[i] || !replySections[i].contains(event.target)) &&
            replyField.value === ''
        ) {
            mediaField.value = null;
            while (mediaPreview.firstChild) {
                mediaPreview.removeChild(mediaPreview.firstChild);
            }
            replySections[i].style.display = 'none';
        }
    }
});
