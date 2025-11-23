/**
 * Карта событий для SocialSphere
 */

document.addEventListener('DOMContentLoaded', function() {
    // Инициализация карты
    const map = L.map('map').setView([55.7558, 37.6176], 10); // Москва
    
    // Маркер для текущего местоположения пользователя
    let currentLocationMarker = null;
    // Маркер для выбранного местоположения
    let selectedLocationMarker = null;
    // Маркер для временного выбора местоположения
    let locationMarker = null;

    // Добавление тайлов с OpenStreetMap
    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
        attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
    }).addTo(map);

    // Массив для хранения маркеров
    let markers = [];
    let heatLayer = null;

    // Функция для загрузки событий с сервера
    function loadEvents() {
        fetch('/api/events')
            .then(response => response.json())
            .then(data => {
                // Очистка старых маркеров
                clearMarkers();
                clearHeatmap();

                // Добавление новых маркеров
                data.events.forEach(event => {
                    if (event.latitude && event.longitude) {
                        // Создание маркера
                        const marker = L.marker([event.latitude, event.longitude], {
                            title: event.title
                        }).addTo(map);

                        // Добавление всплывающего окна
                        marker.bindPopup(`
                            <div style="min-width: 200px;">
                                <h3>${event.title}</h3>
                                <p><strong>Тип:</strong> ${event.type}</p>
                                <p><strong>Дата:</strong> ${formatDateTime(event.date_time)}</p>
                                <p><strong>Место:</strong> ${event.location}</p>
                                <p><strong>Организатор:</strong> ${event.creator.first_name} ${event.creator.last_name}</p>
                                <a href="/event/${event.id}" class="btn btn-sm btn-primary">Подробнее</a>
                            </div>
                        `);

                        // Добавление в массив маркеров
                        markers.push(marker);
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
        fetch('/api/heatmap')
            .then(response => response.json())
            .then(data => {
                // Очистка старой тепловой карты
                clearHeatmap();

                // Подготовка данных для тепловой карты
                const heatData = data.points.map(point => [
                    point.latitude,
                    point.longitude,
                    point.intensity
                ]);

                // Создание слоя тепловой карты
                heatLayer = L.heatLayer(heatData, {
                    radius: 25,
                    blur: 15,
                    maxZoom: 18
                }).addTo(map);
            })
            .catch(error => {
                console.error('Ошибка загрузки данных для тепловой карты:', error);
            });
    }

    // Функция для очистки маркеров
    function clearMarkers() {
        markers.forEach(marker => {
            map.removeLayer(marker);
        });
        markers = [];
    }

    // Функция для очистки тепловой карты
    function clearHeatmap() {
        if (heatLayer) {
            map.removeLayer(heatLayer);
            heatLayer = null;
        }
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

    // Функция для загрузки текущего местоположения пользователя
    function loadUserLocation() {
        // Попробуем получить данные пользователя из DOM (если они доступны)
        const currentUserElement = document.querySelector('[data-user-id]');
        if (currentUserElement) {
            const userId = currentUserElement.dataset.userId;
            const userCity = currentUserElement.dataset.userCity;
            const userLat = parseFloat(currentUserElement.dataset.userLatitude) || null;
            const userLng = parseFloat(currentUserElement.dataset.userLongitude) || null;
            
            if (userLat !== null && userLng !== null && userLat !== 0 && userLng !== 0) {
                displayCurrentLocation(userLat, userLng, userCity || 'Неизвестный город');
            }
        } else {
            console.log('Элемент пользователя не найден');
        }
    }
    
    // Отображение текущего местоположения
    function displayCurrentLocation(lat, lng, city) {
        const currentLocationDiv = document.getElementById('current-location');
        const currentCitySpan = document.getElementById('current-city');
        const currentCoordsSpan = document.getElementById('current-coords');
        const setLocationSection = document.getElementById('set-location-section');
        const changeLocationBtn = document.getElementById('change-location-btn');
        const clearBtn = document.getElementById('clear-location-btn');
        
        if (currentLocationDiv && currentCitySpan && currentCoordsSpan) {
            // Всегда показываем текущее местоположение
            currentCitySpan.textContent = city;
            currentCoordsSpan.textContent = `${lat.toFixed(6)}, ${lng.toFixed(6)}`;
            currentLocationDiv.style.display = 'block';
            
            // Показываем кнопку изменения и очистки
            if (changeLocationBtn) changeLocationBtn.style.display = 'inline-flex';
            if (clearBtn) clearBtn.style.display = 'inline-flex';
            
            // Скрываем секцию установки при отображении текущего местоположения
            if (setLocationSection) setLocationSection.style.display = 'none';
            
            // Добавляем или обновляем маркер текущего местоположения
            if (currentLocationMarker) {
                map.removeLayer(currentLocationMarker);
            }
            
            currentLocationMarker = L.marker([lat, lng], {
                icon: L.divIcon({
                    className: 'current-location-marker',
                    html: '🔵',
                    iconSize: [20, 20]
                })
            }).addTo(map);
            
            // Центрируем карту на текущем местоположении
            map.setView([lat, lng], 12);
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
        const locationBtn = document.getElementById('set-location-btn');
        const changeLocationBtn = document.getElementById('change-location-btn');
        const clearBtn = document.getElementById('clear-location-btn');
        const currentLocationDiv = document.getElementById('current-location');
        const currentCitySpan = document.getElementById('current-city');
        const currentCoordsSpan = document.getElementById('current-coords');
        const setLocationSection = document.getElementById('set-location-section');
        const locationInfo = document.getElementById('location-info');
        const locationCoords = document.getElementById('location-coords');
        const locationCity = document.getElementById('location-city');
        
        let selectedLocation = null;
        
        // Загружаем текущее местоположение пользователя при инициализации
        loadUserLocation();
        
        // Добавляем обработчик клика по карте
        map.on('click', function(e) {
            console.log('Карта кликнута:', e.latlng);
            
            selectedLocation = {
                lat: e.latlng.lat,
                lng: e.latlng.lng
            };
            
            // Удаляем предыдущий маркер
            if (locationMarker) {
                map.removeLayer(locationMarker);
            }
            
            // Добавляем новый маркер
            locationMarker = L.marker([e.latlng.lat, e.latlng.lng], {
                icon: L.divIcon({
                    className: 'user-location-marker',
                    html: '📍',
                    iconSize: [20, 20]
                })
            }).addTo(map);
            
            // Обновляем информацию
            locationCoords.textContent = `${e.latlng.lat.toFixed(6)}, ${e.latlng.lng.toFixed(6)}`;
            locationCity.textContent = 'Определяется...';
            locationInfo.style.display = 'block';
            
            // Убеждаемся, что секция с информацией видима
            if (setLocationSection) {
                setLocationSection.style.display = 'block';
            }
            
            // Удаляем предыдущий временный маркер, если есть
            if (selectedLocationMarker) {
                map.removeLayer(selectedLocationMarker);
            }
            
            // Создаем временный маркер для визуального подтверждения
            selectedLocationMarker = L.marker([e.latlng.lat, e.latlng.lng], {
                icon: L.divIcon({
                    className: 'selected-location-marker',
                    html: '🎯',
                    iconSize: [20, 20]
                })
            }).addTo(map);
            
            // Пытаемся определить город через reverse geocoding
            fetch(`https://nominatim.openstreetmap.org/reverse?format=json&lat=${e.latlng.lat}&lon=${e.latlng.lng}&zoom=10&addressdetails=1`)
                .then(response => {
                    if (!response.ok) {
                        throw new Error(`HTTP error! status: ${response.status}`);
                    }
                    return response.json();
                })
                .then(data => {
                    console.log('Ответ от Nominatim:', data);
                    if (data && data.address) {
                        const city = data.address.city || 
                                 data.address.town || 
                                 data.address.village || 
                                 data.address.hamlet || 
                                 data.address.county || 
                                 data.address.state || 
                                 'Неизвестный населенный пункт';
                        locationCity.textContent = city;
                    } else {
                        locationCity.textContent = 'Не удалось определить город';
                    }
                })
                .catch(error => {
                    console.error('Ошибка определения местоположения:', error);
                    locationCity.textContent = 'Ошибка определения';
                });
        });
        
        // Обработчик подтверждения местоположения
        if (locationBtn) {
            locationBtn.addEventListener('click', function() {
                if (!selectedLocation) {
                    alert('Сначала выберите место на карте кликом');
                    return;
                }
                
                const cityName = locationCity.textContent !== 'Определяется...' && 
                              locationCity.textContent !== 'Ошибка определения' && 
                              locationCity.textContent !== 'Не удалось определить город' ? 
                    locationCity.textContent : 'Неизвестный город';
                
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
                        // Скрываем форму установки и показываем текущее местоположение
                        if (setLocationSection) setLocationSection.style.display = 'none';
                        if (currentLocationDiv) {
                            currentLocationDiv.style.display = 'block';
                            currentCitySpan.textContent = cityName;
                            currentCoordsSpan.textContent = `${selectedLocation.lat.toFixed(6)}, ${selectedLocation.lng.toFixed(6)}`;
                        }
                        // Гарантируем, что кнопки "Изменить" и "Очистить" видимы
                        if (changeLocationBtn) {
                            changeLocationBtn.style.display = 'inline-flex';
                            changeLocationBtn.disabled = false;
                        }
                        if (clearBtn) {
                            clearBtn.style.display = 'inline-flex';
                            clearBtn.disabled = false;
                        }
                        
                        // Обновляем маркер текущего местоположения
                        if (currentLocationMarker) {
                            map.removeLayer(currentLocationMarker);
                        }
                        currentLocationMarker = L.marker([selectedLocation.lat, selectedLocation.lng], {
                            icon: L.divIcon({
                                className: 'current-location-marker',
                                html: '🔵',
                                iconSize: [20, 20]
                            })
                        }).addTo(map);
                        
                        // Удаляем временный маркер
                        if (selectedLocationMarker) {
                            map.removeLayer(selectedLocationMarker);
                            selectedLocationMarker = null;
                        }
                        
                        // Центрируем карту на новом местоположении
                        map.setView([selectedLocation.lat, selectedLocation.lng], 12);
                        
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
        
        // Обработчик кнопки изменения местоположения
        if (changeLocationBtn) {
            changeLocationBtn.addEventListener('click', function() {
                // Скрываем текущее местоположение и показываем форму установки
                if (currentLocationDiv) currentLocationDiv.style.display = 'none';
                if (setLocationSection) setLocationSection.style.display = 'block';
                // Сохраняем кнопку "Изменить" видимой, но неактивной
                if (changeLocationBtn) changeLocationBtn.style.display = 'inline-flex';
                if (clearBtn) clearBtn.style.display = 'inline-flex';
                if (locationInfo) locationInfo.style.display = 'none';
                
                // Очищаем предыдущий выбор
                if (selectedLocationMarker) {
                    map.removeLayer(selectedLocationMarker);
                    selectedLocationMarker = null;
                }
                selectedLocation = null;
                locationInfo.style.display = 'none';
                locationCoords.textContent = '';
                locationCity.textContent = '';
                
                // Центрируем карту на текущем местоположении для удобства выбора
                if (currentLocationMarker) {
                    map.setView(currentLocationMarker.getLatLng(), 12);
                } else {
                    // Если текущего местоположения нет, центрируем на Москве
                    map.setView([55.7558, 37.6176], 10);
                }
            });
        }
        
        // Обработчик очистки местоположения
        if (clearBtn) {
            clearBtn.addEventListener('click', function() {
                if (locationMarker) {
                    map.removeLayer(locationMarker);
                    locationMarker = null;
                }
                selectedLocation = null;
                locationInfo.style.display = 'none';
                locationCoords.textContent = '';
                locationCity.textContent = '';
                
                alert('Местоположение очищено');
            });
        }
        
        console.log('Контролы местоположения инициализированы');
    }

    // Инициализация компонентов карты
    setupMapControls();
    setupLocationControls();

    // Загрузка данных при инициализации (режим событий по умолчанию)
    loadEvents();

    // Устанавливаем активную кнопку
    const eventsButton = document.querySelector('[data-mode="events"]');
    if (eventsButton) {
        eventsButton.classList.add('btn-active');
    }
});
