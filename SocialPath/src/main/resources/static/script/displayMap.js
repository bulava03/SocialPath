document.addEventListener('DOMContentLoaded', function() {
    const mapElement = document.getElementById('displayMap');

    if (mapElement) {
        const lat = parseFloat(mapElement.getAttribute('data-lat'));
        const lng = parseFloat(mapElement.getAttribute('data-lng'));

        if (!isNaN(lat) && !isNaN(lng)) {
            // Ініціалізація карти
            const map = L.map('displayMap').setView([lat, lng], 15);

            // Додавання шару OpenStreetMap
            L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
            }).addTo(map);

            // Додавання маркера
            const marker = L.marker([lat, lng]).addTo(map);

            // Додавання попапу з адресою (за бажанням)
            const address = mapElement.getAttribute('data-address');
            if (address) {
                marker.bindPopup(address).openPopup();
            }
        }
    }
});
