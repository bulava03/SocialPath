function makeVisibleReport(commentId) {
    var replySections = document.getElementsByClassName('report-section');
    for (var i = 0; i < replySections.length; i++) {
        var replyField = replySections[i].getElementsByClassName('report-field')[0];
        if (replyField.value === '') {
            replySections[i].style.display = 'none';
        }
    }

    var targetReplySection = document.getElementsByClassName('report-section ' + commentId)[0];
    if (targetReplySection) {
        targetReplySection.style.display = 'block';
    }
}

document.addEventListener('click', function (event) {
    if (event.target.classList.contains('report-btn-t1')) {
        return;
    }

    if (event.target.classList.contains('report-btn-t2')) {
        return;
    }

    var replySections = document.getElementsByClassName('report-section');
    for (var i = 0; i < replySections.length; i++) {
        var replyField = replySections[i].getElementsByClassName('report-field')[0];
        if (
            (event.target === replySections[i] || !replySections[i].contains(event.target)) &&
            replyField.value === ''
        ) {
            replySections[i].style.display = 'none';
        }
    }
});
