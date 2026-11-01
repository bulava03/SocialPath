// The profile form: the line under each "Who sees it" list says in full who
// will see the contact with the option chosen there.
document.querySelectorAll("select[data-visibility-hint]").forEach(select => {
    const hint = document.getElementById(select.dataset.visibilityHint);
    select.addEventListener("change", () => {
        hint.textContent = select.selectedOptions[0].dataset.hint;
    });
});
