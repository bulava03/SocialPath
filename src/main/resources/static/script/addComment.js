function sendReply(commentId) {
    var idPublication = document.getElementById('idPublication');
    var commentText = document.getElementById('commentText');

    var replyElements = document.getElementsByClassName('reply-section');

    for (var i = 0; i < replyElements.length; i++) {
        var element = replyElements[i];
        if (element.classList.contains(commentId)) {
            var commentTextElement = element.querySelector('.reply-field');
            var idPublication = document.getElementById('idPublication');

            idPublication.value = commentId;
            commentText.value = commentTextElement.value;

            var replySections = document.getElementsByClassName('reply-section');
            for (var i = 0; i < replySections.length; i++) {
                var replyField = replySections[i].getElementsByClassName('reply-field')[0];
                if (replyField.value === '') {
                    replySections[i].style.display = 'none';
                }
            }

            element.querySelector('.reply-field').value = '';
            element.style.display = 'none';

            document.getElementById('commentSender').submit();
            break;
        }
    }
}
