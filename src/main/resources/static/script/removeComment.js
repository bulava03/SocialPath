function removeComment(commentId) {
    var comment = document.getElementById(commentId);
    var idCommentRemove = document.getElementById('idCommentRemove');
    idCommentRemove.value = comment.id;

    var publication = comment.parentNode;
    var idPublicationRemove = document.getElementById('idPublicationRemove');
    idPublicationRemove.value = publication.id;

    document.getElementById('commentRemover').submit();
}
