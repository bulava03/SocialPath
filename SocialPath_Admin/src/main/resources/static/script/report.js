function setReportResult(reportId) {
    const form = document.getElementById("ReportResult");
    var toSet = document.getElementById("myReport");
    toSet.value = reportId;
    form.submit();
}

function getNewReport(reportId) {
    const form = document.getElementById("NewReport");
    var toSet = document.getElementById("freeReport");
    toSet.value = reportId;
    form.submit();
}
