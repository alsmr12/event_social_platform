/**
 * Карта событий для SocialSphere
 */

// Глобальные переменные
let map = null;
let currentLocationMarker = null;
let selectedLocationMarker = null;
let locationMarker = null;
let selectedLocation = null;
let heatmap = null;
let isHeatmapMode = false;

// Функция для перевода типа события на русский
function getTypeInRussian(type) {
    const typeMap = {
        'concert': 'Концерт',
        'lecture': 'Лекция',
        'sport': 'Спорт',
        'meeting': 'Встреча',
        'party': 'Вечеринка',
        'conference': 'Конференция',
        'exhibition': 'Выставка',
        'other': 'Другое'
    };
    return typeMap[type] || type;
}

// Инициализация карты после загрузки API Яндекс.Карт
ymaps.ready(initMap);

function initMap() {
    // Создание экземпляра карты
    map = new ymaps.Map('map', {
        center: [55.7558, 37.6176],
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

// Функция для создания маркера текущего местоположения
function createCurrentLocationMarker(coords) {
    return new ymaps.Placemark(coords, {}, {
        preset: 'islands#blueCircleIcon',
        iconColor: '#1e88e5',
        iconCaptionMaxWidth: '200'
    });
}

// Функция для создания маркера выбранного местоположения
function createSelectedLocationMarker(coords) {
    return new ymaps.Placemark(coords, {
        balloonContent: 'Выбранное местоположение'
    }, {
        preset: 'islands#redIcon',
        draggable: false
    });
}

// Функция для создания временного маркера
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
    console.log('Загрузка событий...');

    // Очищаем тепловую карту если она активна
    if (heatmap) {
        map.geoObjects.remove(heatmap);
        heatmap = null;
    }
    isHeatmapMode = false;

    fetch('/api/events')
        .then(response => response.json())
        .then(data => {
            console.log('Получены события:', data.events.length);
            map.geoObjects.removeAll();

            // Восстанавливаем маркеры местоположения
            if (currentLocationMarker) map.geoObjects.add(currentLocationMarker);
            if (selectedLocationMarker) map.geoObjects.add(selectedLocationMarker);
            if (locationMarker) map.geoObjects.add(locationMarker);

            // Добавление новых меток событий
            data.events.forEach(event => {
                if (event.latitude && event.longitude) {
                    const placemark = new ymaps.Placemark([
                        event.latitude,
                        event.longitude
                    ], {
                        balloonContent: `<div style="min-width: 200px;">
                            <h3>${event.title}</h3>
                            <p><strong>Тип:</strong> ${getTypeInRussian(event.type)}</p>
                            <p><strong>Дата:</strong> ${formatDateTime(event.date_time)}</p>
                            <p><strong>Место:</strong> ${event.location}</p>
                            <p><strong>Организатор:</strong> ${event.creator.first_name} ${event.creator.last_name}</p>
                            <a href="/event/${event.id}" class="btn btn-sm btn-primary">Подробнее</a>
                        </div>`
                    }, {
                        preset: 'islands#blueDotIcon'
                    });

                    map.geoObjects.add(placemark);
                }
            });

            hideError();
        })
        .catch(error => {
            console.error('Ошибка загрузки событий:', error);
            showError('Не удалось загрузить события');
        });
}

// Функция для загрузки данных для тепловой карты
function loadHeatmapData() {
    console.log('Загрузка тепловой карты...');

    // Очистка всех объектов карты
    map.geoObjects.removeAll();
    isHeatmapMode = true;

    // Восстанавливаем только маркеры местоположения
    if (currentLocationMarker) map.geoObjects.add(currentLocationMarker);
    if (selectedLocationMarker) map.geoObjects.add(selectedLocationMarker);
    if (locationMarker) map.geoObjects.add(locationMarker);

    showError('Загрузка данных для тепловой карты...');

    fetch('/api/heatmap')
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            console.log('Получены данные тепловой карты:', data);
            hideError();

            if (!data.points || data.points.length === 0) {
                console.warn('Нет данных для отображения тепловой карты');
                showError('Нет данных для отображения тепловой карты');
                return;
            }

            // В Яндекс.Картах тепловые карты реализуются через кластеризатор с разными цветами
            // Создаем кластеризатор для точек с разной интенсивностью
            const clusterer = new ymaps.Clusterer({
                preset: 'islands#invertedVioletClusterIcons',
                clusterDisableClickZoom: true,
                clusterOpenBalloonOnClick: true,
                clusterBalloonContentLayout: 'cluster#balloonCarousel',
                clusterBalloonItemContentLayout: 'hotspot#balloonItemContentLayout',
                clusterBalloonPanelMaxMapArea: 0,
                clusterBalloonContentLayoutWidth: 300,
                clusterBalloonContentLayoutHeight: 200,
                clusterBalloonPagerSize: 5
            });

            // Создаем коллекцию объектов для кластеризатора
            const geoObjects = [];

            data.points.forEach((point, index) => {
                // Определяем цвет и размер метки в зависимости от интенсивности
                const intensity = point.intensity || 0.1;
                let color, size;

                if (intensity < 0.3) {
                    color = '#00ff00'; // зеленый
                    size = 'small';
                } else if (intensity < 0.6) {
                    color = '#ffff00'; // желтый
                    size = 'medium';
                } else {
                    color = '#ff0000'; // красный
                    size = 'large';
                }

                const placemark = new ymaps.Placemark([point.latitude, point.longitude], {
                    balloonContent: `
                        <div style="min-width: 200px;">
                            <p><strong>Событий:</strong> ${point.eventCount || 0}</p>
                            <p><strong>Участников:</strong> ${point.participantCount || 0}</p>
                            <p><strong>Постов:</strong> ${point.count || 0}</p>
                            ${point.events ? `<div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee;"><strong>События в этой области:</strong><ul style="margin: 5px 0; padding-left: 20px; font-size: 0.9em;">
                                ${point.events.map(event => `<li><a href="/event/${event.id}" style="color: #1e88e5; text-decoration: none;" onclick="event.stopPropagation();">${event.title}</a> <span style="color: #666;">(${getTypeInRussian(event.type)}, ${event.date})</span></li>`).join('')}
                            </ul></div>` : ''}
                        </div>
                    `,
                    hotspotContent: `Активность: ${(intensity * 100).toFixed(1)}%`
                }, {
                    preset: `islands#${color}${size.charAt(0).toUpperCase() + size.slice(1)}Icon`,
                    balloonCloseButton: true,
                    hideIconOnBalloonOpen: false
                });

                geoObjects.push(placemark);
            });

            // Добавляем объекты в кластеризатор
            clusterer.add(geoObjects);

            // Добавляем кластеризатор на карту
            map.geoObjects.add(clusterer);

            // Автоматическое масштабирование под данные
            if (data.points.length > 0) {
                const coordinates = data.points.map(point => [point.latitude, point.longitude]);
                map.setBounds(ymaps.util.bounds.fromPoints(coordinates), {
                    checkZoomRange: true,
                    zoomMargin: 30
                });
            }

            console.log('Тепловая карта создана через кластеризатор');
        })
        .catch(error => {
            console.error('Ошибка загрузки данных для тепловой карты:', error);
            showError('Не удалось загрузить данные для тепловой карты: ' + error.message);
        });
}

// Альтернативная версия - создаем тепловую карту через градиентные круги
function loadHeatmapDataAlternative() {
    console.log('Загрузка тепловой карты (альтернативный метод)...');

    map.geoObjects.removeAll();
    isHeatmapMode = true;

    if (currentLocationMarker) map.geoObjects.add(currentLocationMarker);
    if (selectedLocationMarker) map.geoObjects.add(selectedLocationMarker);
    if (locationMarker) map.geoObjects.add(locationMarker);

    showError('Загрузка данных для тепловой карты...');

    fetch('/api/heatmap')
        .then(response => response.json())
        .then(data => {
            console.log('Получены данные тепловой карты:', data);
            hideError();

            if (!data.points || data.points.length === 0) {
                showError('Нет данных для отображения тепловой карты');
                return;
            }

            // Создаем круги разного цвета в зависимости от интенсивности
            data.points.forEach(point => {
                const intensity = point.intensity || 0.1;
                let color, radius;

                // Используем разные оттенки желтого до оранжевого в зависимости от интенсивности
                if (intensity < 0.3) {
                    color = 'rgba(255, 255, 100, 0.4)'; // светло-желтый
                } else if (intensity < 0.6) {
                    color = 'rgba(255, 220, 0, 0.5)'; // желтый
                } else {
                    color = 'rgba(255, 165, 0, 0.6)'; // оранжевый
                }
                
                // Увеличиваем радиус для лучшего перекрытия
                radius = 10000; // 10km

                // Создаем круг для тепловой карты
                const circle = new ymaps.Circle([
                    [point.latitude, point.longitude], // центр
                    radius // радиус
                ], {
                    balloonContent: `
                        <div style="min-width: 200px;">
                            <p><strong>Событий:</strong> ${point.eventCount || 0}</p>
                            <p><strong>Участников:</strong> ${point.participantCount || 0}</p>
                            <p><strong>Постов:</strong> ${point.count || 0}</p>
                            ${point.events ? `<div style="margin-top: 10px; padding-top: 10px; border-top: 1px solid #eee;"><strong>События в этой области:</strong><ul style="margin: 5px 0; padding-left: 20px; font-size: 0.9em;">
                                ${point.events.map(event => `<li><a href="/event/${event.id}" style="color: #1e88e5; text-decoration: none;" onclick="event.stopPropagation();">${event.title}</a> <span style="color: #666;">(${getTypeInRussian(event.type)}, ${event.date})</span></li>`).join('')}
                            </ul></div>` : ''}
                        </div>
                    `
                }, {
                    fillColor: color,
                    strokeColor: color.replace('0.3', '0.8').replace('0.4', '0.8').replace('0.5', '0.8'),
                    strokeWidth: 2,
                    strokeOpacity: 0.8
                });

                map.geoObjects.add(circle);
            });

            // Автоматическое масштабирование
            if (data.points.length > 0) {
                const coordinates = data.points.map(point => [point.latitude, point.longitude]);
                map.setBounds(ymaps.util.bounds.fromPoints(coordinates), {
                    checkZoomRange: true,
                    zoomMargin: 30
                });
            }



            console.log('Тепловая карта создана через цветные круги');
        })
        .catch(error => {
            console.error('Ошибка загрузки данных для тепловой карты:', error);
            showError('Не удалось загрузить данные для тепловой карты: ' + error.message);
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

// Функция для скрытия сообщения об ошибке
function hideError() {
    const errorElement = document.getElementById('map-error');
    if (errorElement) {
        errorElement.style.display = 'none';
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

            if (mode === 'events') {
                loadEvents();
            } else if (mode === 'heatmap') {
                // Используем альтернативный метод для тепловой карты
                loadHeatmapDataAlternative();
            }
        }
    });
}

// Обработчик установки местоположения пользователя
function setupLocationControls() {
    const setLocationSection = document.getElementById('set-location-section');
    const currentLocationDiv = document.getElementById('current-location');
    const locationCoords = document.getElementById('location-coords');
    const locationCity = document.getElementById('location-city');
    const currentCitySpan = document.getElementById('current-city');
    const currentCoordsSpan = document.getElementById('current-coords');

    // Кнопка для определения местоположения
    const geoBtn = document.getElementById('auto-location-btn');
    if (geoBtn) {
        geoBtn.addEventListener('click', function() {
            if (!navigator.geolocation) {
                alert('Геолокация не поддерживается вашим браузером');
                return;
            }

            const originalText = geoBtn.innerHTML;
            geoBtn.innerHTML = 'GPS...';
            geoBtn.disabled = true;

            navigator.geolocation.getCurrentPosition(
                function(position) {
                    geoBtn.innerHTML = originalText;
                    geoBtn.disabled = false;

                    const lat = position.coords.latitude;
                    const lng = position.coords.longitude;
                    selectedLocation = { lat, lng };

                    // Удаляем предыдущие временные маркеры
                    if (locationMarker) map.geoObjects.remove(locationMarker);
                    if (selectedLocationMarker) map.geoObjects.remove(selectedLocationMarker);

                    // Добавляем временный маркер
                    locationMarker = createTemporaryMarker([lat, lng]);
                    map.geoObjects.add(locationMarker);

                    // Обновляем информацию
                    if (locationCoords) locationCoords.textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
                    if (locationCity) locationCity.textContent = 'Определяется...';
                    if (setLocationSection) setLocationSection.style.display = 'block';

                    // Определяем город
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

                    map.setCenter([lat, lng], 12);
                },
                function(error) {
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

        if (setLocationSection) setLocationSection.style.display = 'block';

        selectedLocation = {
            lat: coords[0],
            lng: coords[1]
        };

        // Удаляем предыдущие временные маркеры
        if (locationMarker) map.geoObjects.remove(locationMarker);
        if (selectedLocationMarker) map.geoObjects.remove(selectedLocationMarker);

        // Добавляем новый временный маркер
        locationMarker = createTemporaryMarker([coords[0], coords[1]]);
        map.geoObjects.add(locationMarker);

        // Обновляем информацию
        if (locationCoords) locationCoords.textContent = `${coords[0].toFixed(6)}, ${coords[1].toFixed(6)}`;
        if (locationCity) locationCity.textContent = 'Определяется...';

        // Определение города
        ymaps.geocode(coords, {
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

                        if (currentLocationDiv) {
                            currentLocationDiv.style.display = 'block';
                            if (currentCitySpan) currentCitySpan.textContent = cityName;
                            if (currentCoordsSpan) currentCoordsSpan.textContent = `${selectedLocation.lat.toFixed(6)}, ${selectedLocation.lng.toFixed(6)}`;
                        }

                        map.setCenter([selectedLocation.lat, selectedLocation.lng], 12);

                        // Перезагружаем данные в зависимости от текущего режима
                        if (isHeatmapMode) {
                            loadHeatmapDataAlternative();
                        } else {
                            loadEvents();
                        }
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

    if (setLocationSection) setLocationSection.style.display = 'block';
    if (currentLocationDiv) currentLocationDiv.style.display = 'block';

    function showCurrentLocation() {
        const userAttrs = document.querySelector('[data-user-id]');
        if (userAttrs) {
            const lat = parseFloat(userAttrs.dataset.userLatitude) || null;
            const lng = parseFloat(userAttrs.dataset.userLongitude) || null;
            const city = userAttrs.dataset.userCity || 'Неизвестный город';

            if (lat !== null && lng !== null && lat !== 0 && lng !== 0) {
                displayCurrentLocation(lat, lng, city);
            } else {
                if (setLocationSection) setLocationSection.style.display = 'block';
            }
        } else {
            if (setLocationSection) setLocationSection.style.display = 'block';
        }
    }

    function displayCurrentLocation(lat, lng, city) {
        if (currentLocationDiv && currentCitySpan && currentCoordsSpan) {
            currentCitySpan.textContent = city;
            currentCoordsSpan.textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            currentLocationDiv.style.display = 'block';

            if (currentLocationMarker) {
                map.geoObjects.remove(currentLocationMarker);
            }

            currentLocationMarker = createCurrentLocationMarker([lat, lng]);
            map.geoObjects.add(currentLocationMarker);
            map.setCenter([lat, lng], 12);
        }
    }

    console.log('Контролы местоположения инициализированы');
}