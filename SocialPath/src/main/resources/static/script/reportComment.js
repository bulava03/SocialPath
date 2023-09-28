function sendReport(commentId) {
    var idPublication = document.getElementById('idPublicationReport');
    var idComment = document.getElementById('idCommentReport');
    var commentText = document.getElementById('commentTextReport');

    var replyElements = document.getElementsByClassName('report-section');

    for (var i = 0; i < replyElements.length; i++) {
        var element = replyElements[i];
        if (element.classList.contains(commentId)) {
            var commentTextElement = element.querySelector('.report-field');
            var idPublication = document.getElementById('idPublicationReport');

            var publication = document.getElementById(commentId).parentNode.parentNode.parentNode.parentNode;
            var publicationId = publication.id;

            idPublication.value = publicationId;
            idComment.value = commentId;
            commentText.value = commentTextElement.value;

            var replySections = document.getElementsByClassName('report-section');
            for (var i = 0; i < replySections.length; i++) {
                var replyField = replySections[i].getElementsByClassName('report-field')[0];
                if (replyField.value === '') {
                    replySections[i].style.display = 'none';
                }
            }

            element.querySelector('.report-field').value = '';
            element.style.display = 'none';

            document.getElementById('commentReporter').submit();
            break;
        }
    }
}
