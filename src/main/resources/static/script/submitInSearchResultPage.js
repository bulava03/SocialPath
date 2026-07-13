function submitUser(login) {
    document.getElementById("anotherUserLogin").value = login;
    document.getElementById("userToSubmit").submit();
}

function submitGroup(groupId) {
    document.getElementById("groupId").value = groupId;
    document.getElementById("groupToSubmit").submit();
}
