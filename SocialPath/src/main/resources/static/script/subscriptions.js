function getFormData() {
    return {
        login: document.getElementById("userLogin").value
    };
}

function handleSubscribeResponse(responseCode) {
    if (responseCode === -1) {
        alert("Помилка. Не вдалося підписатися на сторінку.");
    } else if (responseCode === 0 || responseCode === 1) {
        const button = document.getElementById("submitSubscribe");
        button.id = "submitUnsubscribe";
        button.setAttribute("onclick", "unsubscribe()");
        button.textContent = "Відписатися";
    }
}

function handleUnsubscribeResponse(responseCode) {
    if (responseCode === -1) {
        alert("Помилка. Не вдалося відписатися від сторінки.");
    } else if (responseCode === 0) {
        const removeButton = document.getElementById("submitUnsubscribe");
        removeButton.id = "submitSubscribe";
        removeButton.setAttribute("onclick", "subscribe()");
        removeButton.textContent = "Слідкувати";
    }
}

function sendPostRequest(url, data, handleResponseFunction) {
    const formData = new FormData();
    formData.append("login", data.login);

    fetch(url, {
        method: "POST",
        body: formData
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        } else {
            console.log("Помилка сервера: " + response.status);
            return null;
        }
    })
    .then(responseCode => {
        if (responseCode !== null) {
            handleResponseFunction(responseCode);
        }
    })
    .catch(error => {
        console.log("Помилка з'єднання:", error);
    });
}

function subscribe() {
    const data = getFormData();
    sendPostRequest("/subscription/subscribe", data, handleSubscribeResponse);
}

function unsubscribe() {
    const data = getFormData();
    sendPostRequest("/subscription/unsubscribe", data, handleUnsubscribeResponse);
}
