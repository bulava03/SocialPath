function submitFormSetResult(str) {
    const form = document.getElementById("setReportDecisionResult");
    var result = document.getElementById("reportResult");
    result.value = str;
    form.submit();
}

function submitFormBack() {
    const form = document.getElementById("formBack");
    form.submit();
}
