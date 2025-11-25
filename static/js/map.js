/**
 * Карта событий для SocialSphere
 */

// Глобальные переменные
let map = null;
let currentLocationMarker = null;
let selectedLocationMarker = null;
let locationMarker = null;
let selectedLocation = null;

// Инициализация карты после загрузки API Яндекс.Карт
ymaps.ready(initMap);

function initMap() {
    // Создание экземпляра карты
    map = new ymaps.Map('map', {
        center: [55.7558, 37.6176], // Москва
        zoom: 10,
        controls: ['zoomControl', 'fullscreenControl']
    });

    // Загрузка данных при инициализации
    loadEvents();

    // Устанавливаем активную кнопку
    const eventsButton = document.querySelector('[data-mode="events"]');
    if (eventsButton) {
        eventsButton.classList.add('btn-active');
    }

    // Инициализация компонентов
    setupMapControls();
    setupLocationControls();
}

// Функция для создания маркера текущего местоположения (синяя точка как в Яндекс.Картах)
function createCurrentLocationMarker(coords) {
    return new ymaps.Placemark(coords, {
        // Пустой контент, так как нам нужна только точка
    }, {
        // Используем стандартный пресет для текущего местоположения
        preset: 'islands#blueCircleIcon',
        // Делаем точку меньше
        iconColor: '#1e88e5',
        iconCaptionMaxWidth: '200'
    });
}

// Функция для создания маркера выбранного местоположения (красная точка)
function createSelectedLocationMarker(coords) {
    return new ymaps.Placemark(coords, {
        balloonContent: 'Выбранное местоположение'
    }, {
        preset: 'islands#redIcon',
        draggable: false
    });
}

// Функция для создания временного маркера (при клике на карту)
function createTemporaryMarker(coords) {
    return new ymaps.Placemark(coords, {
        balloonContent: 'Новое местоположение'
    }, {
        preset: 'islands#greenIcon',
        draggable: false
    });
}

// Функция для загрузки событий с сервера
function loadEvents() {
    fetch('/api/events')
        .then(response => response.json())
        .then(data => {
            // Очистка старых меток (кроме маркеров местоположения)
            map.geoObjects.removeAll();

            // Восстанавливаем маркеры местоположения
            if (currentLocationMarker) {
                map.geoObjects.add(currentLocationMarker);
            }
            if (selectedLocationMarker) {
                map.geoObjects.add(selectedLocationMarker);
            }
            if (locationMarker) {
                map.geoObjects.add(locationMarker);
            }

            // Добавление новых меток событий
            data.events.forEach(event => {
                if (event.latitude && event.longitude) {
                    // Создание метки события
                    const placemark = new ymaps.Placemark([
                        event.latitude,
                        event.longitude
                    ], {
                        balloonContent: `<div style="min-width: 200px;">
                            <h3>${event.title}</h3>
                            <p><strong>Тип:</strong> ${event.type}</p>
                            <p><strong>Дата:</strong> ${formatDateTime(event.date_time)}</p>
                            <p><strong>Место:</strong> ${event.location}</p>
                            <p><strong>Организатор:</strong> ${event.creator.first_name} ${event.creator.last_name}</p>
                            <a href="/event/${event.id}" class="btn btn-sm btn-primary">Подробнее</a>
                        </div>`
                    }, {
                        preset: 'islands#blueDotIcon'
                    });

                    // Добавление метки на карту
                    map.geoObjects.add(placemark);
                }
            });
        })
        .catch(error => {
            console.error('Ошибка загрузки событий:', error);
            showError('Не удалось загрузить события');
        });
}

// Функция для загрузки данных для тепловой карты
function loadHeatmapData() {
    // Очистка старых объектов (кроме маркеров местоположения)
    const markersToKeep = [];
    if (currentLocationMarker) markersToKeep.push(currentLocationMarker);
    if (selectedLocationMarker) markersToKeep.push(selectedLocationMarker);
    if (locationMarker) markersToKeep.push(locationMarker);

    map.geoObjects.removeAll();

    // Восстанавливаем маркеры местоположения
    markersToKeep.forEach(marker => {
        map.geoObjects.add(marker);
    });

    // Показываем сообщение о загрузке
    showError('Загрузка данных для тепловой карты...');

    fetch('/api/heatmap')
        .then(response => response.json())
        .then(data => {
            // Подготовка данных для тепловой карты
            const heatData = data.points.map(point => [
                point.latitude,
                point.longitude,
                point.intensity
            ]);

            // Создание слоя тепловой карты
            // Проверяем наличие данных перед созданием тепловой карты
            if (heatData.length === 0) {
                console.warn('Нет данных для отображения тепловой карты');
                showError('Нет данных для отображения тепловой карты');
                return;
            }
            
            const heatmap = new ymaps.Heatmap(heatData, {
                radius: 25,
                opacity: 0.8,
                dissipating: true
            });

            // Добавление слоя на карту
            heatmap.setMap(map);
        })
        .catch(error => {
            console.error('Ошибка загрузки данных для тепловой карты:', error);
            showError('Не удалось загрузить данные для тепловой карты');
        });
}

// Функция для форматирования даты и времени
function formatDateTime(dateTimeString) {
    const date = new Date(dateTimeString);
    return date.toLocaleDateString('ru-RU') + ' ' + date.toLocaleTimeString('ru-RU', {
        hour: '2-digit',
        minute: '2-digit'
    });
}

// Функция для отображения ошибки
function showError(message) {
    const errorElement = document.getElementById('map-error');
    if (errorElement) {
        errorElement.textContent = message;
        errorElement.style.display = 'block';
    }
}

// Обработчик переключения режимов карты
function setupMapControls() {
    const controls = document.getElementById('map-controls');
    if (!controls) return;

    controls.addEventListener('click', function(e) {
        if (e.target.classList.contains('btn')) {
            const mode = e.target.dataset.mode;

            // Снимаем активный класс со всех кнопок
            controls.querySelectorAll('.btn').forEach(btn => {
                btn.classList.remove('btn-active');
            });

            // Устанавливаем активный класс на выбранную кнопку
            e.target.classList.add('btn-active');

            // Загружаем соответствующие данные
            if (mode === 'events') {
                loadEvents();
            } else if (mode === 'heatmap') {
                loadHeatmapData();
            }
        }
    });
}

// Обработчик установки местоположения пользователя
function setupLocationControls() {
    // Получаем DOM элементы
    const setLocationSection = document.getElementById('set-location-section');
    const currentLocationDiv = document.getElementById('current-location');
    const locationInfo = document.getElementById('location-info');
    const locationCoords = document.getElementById('location-coords');
    const locationCity = document.getElementById('location-city');
    const currentCitySpan = document.getElementById('current-city');
    const currentCoordsSpan = document.getElementById('current-coords');

    // Кнопка для определения местоположения уже существует в HTML, пропускаем создание
    const geoBtn = document.getElementById('auto-location-btn');
    if (geoBtn) {

        // Обработчик кнопки определения местоположения
        geoBtn.addEventListener('click', function() {
            if (!navigator.geolocation) {
                alert('Геолокация не поддерживается вашим браузером');
                return;
            }

            // Показываем индикатор загрузки
            const originalText = geoBtn.innerHTML;
            geoBtn.innerHTML = 'GPS';
            geoBtn.disabled = true;

            navigator.geolocation.getCurrentPosition(
                function(position) {
                    // Восстанавливаем кнопку
                    geoBtn.innerHTML = originalText;
                    geoBtn.disabled = false;

                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;

                    // Устанавливаем выбранное местоположение
                    selectedLocation = { lat, lng };

                    // Удаляем предыдущие временные маркеры
                    if (locationMarker) {
                        map.geoObjects.remove(locationMarker);
                    }
                    if (selectedLocationMarker) {
                        map.geoObjects.remove(selectedLocationMarker);
                    }

                    // Добавляем временный маркер
                    locationMarker = createTemporaryMarker([lat, lng]);
                    map.geoObjects.add(locationMarker);

                    // Обновляем информацию
                    if (locationCoords) locationCoords.textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
                    if (locationCity) locationCity.textContent = 'Определяется...';
                    if (locationInfo) locationInfo.style.display = 'block';

                    // Показываем секцию установки
                    if (setLocationSection) {
                        setLocationSection.style.display = 'block';
                    }

                    // Определяем город через геокодирование
                    ymaps.geocode([lat, lng], {
                        kind: 'locality',
                        results: 1
                    }).then(function(result) {
                        const geoObject = result.geoObjects.get(0);
                        if (geoObject) {
                            const city = geoObject.getLocalities().length ?
                                geoObject.getLocalities()[0] :
                                geoObject.getAdministrativeAreas().length ?
                                    geoObject.getAdministrativeAreas()[0] :
                                    'Неизвестный населенный пункт';
                            if (locationCity) locationCity.textContent = city;
                        } else {
                            if (locationCity) locationCity.textContent = 'Не удалось определить город';
                        }
                    }).catch(function(error) {
                        console.error('Ошибка определения местоположения:', error);
                        if (locationCity) locationCity.textContent = 'Ошибка определения';
                    });

                    // Центрируем карту на определенном местоположении
                    map.setCenter([lat, lng], 12);
                },
                function(error) {
                    // Восстанавливаем кнопку
                    geoBtn.innerHTML = originalText;
                    geoBtn.disabled = false;

                    switch(error.code) {
                        case error.PERMISSION_DENIED:
                            alert('Пользователь отклонил запрос на определение местоположения');
                            break;
                        case error.POSITION_UNAVAILABLE:
                            alert('Информация о местоположении недоступна');
                            break;
                        case error.TIMEOUT:
                            alert('Истекло время запроса на определение местоположения');
                            break;
                        default:
                            alert('Неизвестная ошибка при определении местоположения');
                            break;
                    }
                }
            );
        });
    }

    // Обработчик клика по карте
    map.events.add('click', function(e) {
        const coords = e.get('coords');
        console.log('Карта кликнута:', coords);

        // Показываем секцию установки при клике по карте
        if (setLocationSection) {
            setLocationSection.style.display = 'block';
        }

        // Всегда показываем текущее местоположение
        // if (currentLocationDiv) {
        //     currentLocationDiv.style.display = 'none';
        // }

        selectedLocation = {
            lat: coords[0],
            lng: coords[1]
        };

        // Удаляем предыдущие временные маркеры
        if (locationMarker) {
            map.geoObjects.remove(locationMarker);
        }
        if (selectedLocationMarker) {
            map.geoObjects.remove(selectedLocationMarker);
        }

        // Добавляем новый временный маркер
        locationMarker = createTemporaryMarker([coords[0], coords[1]]);
        map.geoObjects.add(locationMarker);

        // Обновляем информацию
        if (locationCoords) locationCoords.textContent = `${coords[0].toFixed(6)}, ${coords[1].toFixed(6)}`;
        if (locationCity) locationCity.textContent = 'Определяется...';
        if (locationInfo) locationInfo.style.display = 'block';

        // Определение города через reverse geocoding Яндекс
        ymaps.geocode(coords, {
            kind: 'locality',
            results: 1
        }).then(function(result) {
            console.log('Результат геокодирования:', result);
            const geoObject = result.geoObjects.get(0);
            if (geoObject) {
                const city = geoObject.getLocalities().length ?
                    geoObject.getLocalities()[0] :
                    geoObject.getAdministrativeAreas().length ?
                        geoObject.getAdministrativeAreas()[0] :
                        'Неизвестный населенный пункт';
                if (locationCity) locationCity.textContent = city;
            } else {
                if (locationCity) locationCity.textContent = 'Не удалось определить город';
            }
        }).catch(function(error) {
            console.error('Ошибка определения местоположения:', error);
            if (locationCity) locationCity.textContent = 'Ошибка определения';
        });
    });

    // Обработчик подтверждения местоположения
    const locationBtn = document.getElementById('set-location-btn');
    if (locationBtn) {
        locationBtn.addEventListener('click', function() {
            if (!selectedLocation) {
                alert('Сначала выберите место на карте кликом');
                return;
            }

            const cityName = locationCity && locationCity.textContent !== 'Определяется...' &&
            locationCity.textContent !== 'Ошибка определения' &&
            locationCity.textContent !== 'Не удалось определить город' ?
                locationCity.textContent : 'Неизвестный город';

            console.log('Отправка данных на сервер:', {
                latitude: selectedLocation.lat,
                longitude: selectedLocation.lng,
                city: cityName
            });

            // Отправляем данные на сервер
            fetch('/api/user/location', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    latitude: selectedLocation.lat,
                    longitude: selectedLocation.lng,
                    city: cityName
                })
            })
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Ответ от сервера:', data);
                    if (data.message) {
                        alert('Местоположение успешно сохранено!');

                        // Удаляем временные маркеры
                        if (locationMarker) {
                            map.geoObjects.remove(locationMarker);
                            locationMarker = null;
                        }
                        if (selectedLocationMarker) {
                            map.geoObjects.remove(selectedLocationMarker);
                            selectedLocationMarker = null;
                        }

                        // Создаем постоянный маркер текущего местоположения
                        if (currentLocationMarker) {
                            map.geoObjects.remove(currentLocationMarker);
                        }
                        currentLocationMarker = createCurrentLocationMarker([
                            selectedLocation.lat,
                            selectedLocation.lng
                        ]);
                        map.geoObjects.add(currentLocationMarker);

                        // После установки местоположения окно установки должно остаться видимым
                        // if (setLocationSection) setLocationSection.style.display = 'none';
                        if (currentLocationDiv) {
                            currentLocationDiv.style.display = 'block';
                            if (currentCitySpan) currentCitySpan.textContent = cityName;
                            if (currentCoordsSpan) currentCoordsSpan.textContent = `${selectedLocation.lat.toFixed(6)}, ${selectedLocation.lng.toFixed(6)}`;
                        }

                        // Центрируем карту на новом местоположении
                        map.setCenter([selectedLocation.lat, selectedLocation.lng], 12);

                        // Обновляем события с учетом нового местоположения
                        loadEvents();
                    } else {
                        alert('Ошибка: ' + (data.error || 'Неизвестная ошибка'));
                    }
                })
                .catch(error => {
                    console.error('Ошибка сохранения местоположения:', error);
                    alert('Ошибка соединения с сервером. Проверьте консоль разработчика для деталей.');
                });
        });
    }

    // Показываем текущее местоположение при загрузке
    showCurrentLocation();

    // Гарантируем, что обе секции всегда видны при загрузке
    if (setLocationSection) {
        setLocationSection.style.display = 'block';
    }
    if (currentLocationDiv) {
        currentLocationDiv.style.display = 'block';
    }

    // Функция для отображения текущего местоположения
    function showCurrentLocation() {
        // Пытаемся получить данные пользователя из скрытого элемента
        const userAttrs = document.querySelector('[data-user-id]');
        if (userAttrs) {
            const lat = parseFloat(userAttrs.dataset.userLatitude) || null;
            const lng = parseFloat(userAttrs.dataset.userLongitude) || null;
            const city = userAttrs.dataset.userCity || 'Неизвестный город';

            if (lat !== null && lng !== null && lat !== 0 && lng !== 0) {
                displayCurrentLocation(lat, lng, city);
            } else {
                // Если координаты не установлены, показываем секцию установки
                if (setLocationSection) setLocationSection.style.display = 'block';
            }
        } else {
            // Если элемент не найден, показываем секцию установки
            if (setLocationSection) setLocationSection.style.display = 'block';
        }
    }

    // Отображение текущего местоположения
    function displayCurrentLocation(lat, lng, city) {
        if (currentLocationDiv && currentCitySpan && currentCoordsSpan) {
            // Всегда показываем текущее местоположение
            currentCitySpan.textContent = city;
            currentCoordsSpan.textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            currentLocationDiv.style.display = 'block';

            // После установки местоположения секция установки должна оставаться видимой
            // if (setLocationSection) setLocationSection.style.display = 'none';

            // Добавляем или обновляем маркер текущего местоположения
            if (currentLocationMarker) {
                map.geoObjects.remove(currentLocationMarker);
            }

            currentLocationMarker = createCurrentLocationMarker([lat, lng]);
            map.geoObjects.add(currentLocationMarker);

            // Центрируем карту на текущем местоположении
            map.setCenter([lat, lng], 12);
        }
    }

    // Кнопка "Изменить местоположение" удалена, так как она не нужна
    // Оставлена только кнопка "Установить местоположение"

    // Кнопки "Изменить" и "Очистить" удалены, так как они ничего не делают
    // Оставлены только кнопки "Установить местоположение" и "Изменить местоположение"

    console.log('Контролы местоположения инициализированы');
}