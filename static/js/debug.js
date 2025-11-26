// Добавляем отладочный код для диагностики проблемы с тепловой картой
document.addEventListener('DOMContentLoaded', function() {
    console.log('Debug script loaded');
    
    // Проверяем наличие элементов
    const mapControls = document.getElementById('map-controls');
    const mapElement = document.getElementById('map');
    const errorElement = document.getElementById('map-error');
    
    console.log('Elements:', {
        mapControls: mapControls ? 'found' : 'not found',
        mapElement: mapElement ? 'found' : 'not found',
        errorElement: errorElement ? 'found' : 'not found'
    });
    
    // Добавляем обработчик для кнопки тепловой карты
    const heatmapButton = document.querySelector('[data-mode="heatmap"]');
    if (heatmapButton) {
        heatmapButton.addEventListener('click', function() {
            console.log('Heatmap button clicked');
            
            // Проверяем текущего пользователя
            const userAttrs = document.querySelector('[data-user-id]');
            console.log('User attributes element:', userAttrs ? 'found' : 'not found');
            
            if (userAttrs) {
                console.log('User data:', {
                    id: userAttrs.dataset.userId,
                    city: userAttrs.dataset.userCity,
                    latitude: userAttrs.dataset.userLatitude,
                    longitude: userAttrs.dataset.userLongitude
                });
            }
            
            // Пытаемся загрузить данные вручную
            console.log('Attempting to fetch heatmap data...');
            fetch('/api/heatmap')
                .then(response => {
                    console.log('API response received:', response.status, response.statusText);
                    return response.json();
                })
                .then(data => {
                    console.log('API data received:', data);
                    
                    if (data.points && data.points.length > 0) {
                        console.log('Heatmap data received with', data.points.length, 'points');
                        console.log('First point:', data.points[0]);
                    } else {
                        console.log('No heatmap data received');
                        if (data.error) {
                            console.error('API error:', data.error);
                        }
                        if (data.debug) {
                            console.log('Debug info:', data.debug);
                        }
                    }
                })
                .catch(error => {
                    console.error('API fetch error:', error);
                    console.log('Error details:', error.message);
                });
        });
    }
});