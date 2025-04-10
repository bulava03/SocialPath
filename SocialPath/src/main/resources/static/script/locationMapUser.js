document.addEventListener('DOMContentLoaded', function() {
    // Ініціалізація карти (Київ за замовчуванням)
    const defaultLat = 50.450001;
    const defaultLng = 30.523333;
    const defaultZoom = 13;

    let initialLat = document.getElementById('latitude').value != 0
        ? parseFloat(document.getElementById('latitude').value)
        : defaultLat;

    let initialLng = document.getElementById('longitude').value != 0
        ? parseFloat(document.getElementById('longitude').value)
        : defaultLng;

    let map = L.map('map').setView([initialLat, initialLng], defaultZoom);

    // Додавання шару OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // Ініціалізація маркера
    let marker = L.marker([initialLat, initialLng], {
        draggable: true
    }).addTo(map);

    // Оновлення координат при перетягуванні маркера
    function updateCoordinates(lat, lng) {
        document.getElementById('latitude').value = lat.toFixed(6);
        document.getElementById('longitude').value = lng.toFixed(6);
    }

    // Початкові координати
    updateCoordinates(initialLat, initialLng);

    // Оновлення координат при переміщенні маркера
    marker.on('dragend', function(event) {
        let position = marker.getLatLng();
        updateCoordinates(position.lat, position.lng);
        updateAddressFromCoordinates(position.lat, position.lng);
    });

    // Встановлення маркера при кліку на карту
    map.on('click', function(e) {
        marker.setLatLng(e.latlng);
        updateCoordinates(e.latlng.lat, e.latlng.lng);
        updateAddressFromCoordinates(e.latlng.lat, e.latlng.lng);
    });

    // Функція для пошуку адреси за координатами (зворотнє геокодування)
    function updateAddressFromCoordinates(lat, lng) {
        fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${lat}&lon=${lng}&zoom=18&addressdetails=1`)
            .then(response => response.json())
            .then(data => {
                if (data && data.display_name) {
                    document.getElementById('address').value = data.display_name;
                }
            })
            .catch(error => console.error('Помилка отримання адреси:', error));
    }

    // Функція для пошуку координат за адресою
    function searchByAddress() {
        const searchInput = document.getElementById('search-input').value;

        if (!searchInput) return;

        fetch(`https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(searchInput)}&limit=1`)
            .then(response => response.json())
            .then(data => {
                if (data && data.length > 0) {
                    const result = data[0];
                    const lat = parseFloat(result.lat);
                    const lng = parseFloat(result.lon);

                    // Оновлення карти, маркера і форми
                    map.setView([lat, lng], 16);
                    marker.setLatLng([lat, lng]);
                    updateCoordinates(lat, lng);
                    document.getElementById('address').value = result.display_name;
                } else {
                    alert('Адреса не знайдена. Спробуйте інший запит.');
                }
            })
            .catch(error => console.error('Помилка пошуку:', error));
    }

    // Прив'язка пошуку до кнопки
    document.getElementById('search-button').addEventListener('click', searchByAddress);

    // Пошук при натисканні Enter у полі пошуку
    document.getElementById('search-input').addEventListener('keypress', function(e) {
        if (e.key === 'Enter') {
            e.preventDefault();
            searchByAddress();
        }
    });

    // Автоматичний пошук за збереженою адресою при завантаженні сторінки
    const savedAddress = document.getElementById('address').value;
    if (savedAddress != '') {
        document.getElementById('search-input').value = savedAddress;
        // Пошук за введеною адресою при завантаженні
        if (!document.getElementById('latitude').value || !document.getElementById('longitude').value) {
            searchByAddress();
        }
    }
});
