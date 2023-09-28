var currentYear = new Date().getFullYear();
var startYear = 1900;
var selectElement = document.getElementById("dob-year");

for (var year = currentYear; year >= startYear; year--) {
    var optionElement = document.createElement("option");
    optionElement.value = year;
    optionElement.text = year;
    selectElement.add(optionElement);
}
