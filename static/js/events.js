
document.addEventListener('DOMContentLoaded', async function() {
    try {
        const eventsContent = document.getElementById('events-content');
        if (!eventsContent) {
            console.error('Events content element not found');
            return;
        }

        // Получаем параметры фильтрации из URL
        const urlParams = new URLSearchParams(window.location.search);
        const filterType = urlParams.get('type') || 'all';
        const dateFrom = urlParams.get('date_from') || '';
        const dateTo = urlParams.get('date_to') || '';
        const radius = urlParams.get('radius') || '0';
        const timeFilter = urlParams.get('filter') || 'upcoming';

        // Формируем URL для запроса с фильтрами
        let apiUrl = '/api/events/filtered?';
        if (filterType !== 'all') apiUrl += `type=${filterType}&`;
        if (dateFrom) apiUrl += `date_from=${dateFrom}&`;
        if (dateTo) apiUrl += `date_to=${dateTo}&`;
        if (radius !== '0') apiUrl += `radius=${radius}&`;
        apiUrl += `filter=${timeFilter}`;

        console.log('Fetching events from:', apiUrl);

        // Получаем данные событий
        const response = await fetch(apiUrl);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Ошибка загрузки событий');
        }

        const events = data.events || [];
        const currentUser = window.currentUser;

        // Создаем HTML-содержимое
        eventsContent.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
                <h2>Все события</h2>
            </div>

            <!-- Форма для кода приглашения -->
            <div class="card" style="margin-bottom: 20px; background: var(--light-bg);">
                <h3 style="margin-top: 0;">🔑 Доступ по коду приглашения</h3>
                <p style="margin-bottom: 15px; color: var(--text-muted);">
                    Есть код приглашения? Введите его ниже чтобы получить доступ к приватному событию.
                </p>
                <form id="invite-form" style="display: flex; gap: 10px; align-items: end;">
                    <div style="flex: 1;">
                        <label for="code" style="display: block; margin-bottom: 5px; font-weight: 600;">Код приглашения:</label>
                        <input type="text" id="code" name="code" placeholder="Введите код приглашения" class="form-control" required>
                    </div>
                    <button type="submit" class="btn btn-primary">Перейти к событию</button>
                </form>
            </div>

            <!-- Форма фильтрации -->
            <div class="card" style="margin-bottom: 20px; background: var(--light-bg);">
                <h3 style="margin-bottom: 15px;">🔍 Фильтры событий</h3>
                <form id="filter-form" class="filter-form">
                    <input type="hidden" name="filter" value="${timeFilter}">
                    
                    <div style="display: grid; grid-template-columns: repeat(auto-fit, minmax(200px, 1fr)); gap: 15px; margin-bottom: 15px;">
                        <!-- Фильтр по типу -->
                        <div>
                            <label style="display: block; margin-bottom: 5px; font-weight: 600;">Тип события</label>
                            <select name="type" class="form-control">
                                <option value="all" ${filterType === 'all' ? 'selected' : ''}>Все типы</option>
                                <option value="concert" ${filterType === 'concert' ? 'selected' : ''}>Концерт</option>
                                <option value="lecture" ${filterType === 'lecture' ? 'selected' : ''}>Лекция</option>
                                <option value="sport" ${filterType === 'sport' ? 'selected' : ''}>Спорт</option>
                                <option value="meeting" ${filterType === 'meeting' ? 'selected' : ''}>Встреча</option>
                                <option value="party" ${filterType === 'party' ? 'selected' : ''}>Вечеринка</option>
                                <option value="conference" ${filterType === 'conference' ? 'selected' : ''}>Конференция</option>
                                <option value="exhibition" ${filterType === 'exhibition' ? 'selected' : ''}>Выставка</option>
                                <option value="other" ${filterType === 'other' ? 'selected' : ''}>Другое</option>
                            </select>
                        </div>

                        <!-- Фильтр по дате "от" -->
                        <div>
                            <label style="display: block; margin-bottom: 5px; font-weight: 600;">Дата с</label>
                            <input type="date" name="date_from" value="${dateFrom}" class="form-control">
                        </div>

                        <!-- Фильтр по дате "до" -->
                        <div>
                            <label style="display: block; margin-bottom: 5px; font-weight: 600;">Дата по</label>
                            <input type="date" name="date_to" value="${dateTo}" class="form-control">
                        </div>

                        <!-- Фильтр по радиусу -->
                        <div>
                            <label style="display: block; margin-bottom: 5px; font-weight: 600;">Радиус (км)</label>
                            <input type="number" name="radius" value="${radius}" class="form-control" placeholder="0" min="0" step="1">
                            <small style="color: var(--text-muted); font-size: 0.8rem;">0 = любой радиус</small>
                        </div>
                    </div>

                    <div style="display: flex; gap: 10px;">
                        <button type="submit" class="btn btn-primary">Применить фильтры</button>
                        <button type="button" id="reset-filters" class="btn btn-outline">Сбросить фильтры</button>
                    </div>
                </form>
            </div>

            <!-- Вкладки -->
            <div class="tabs" style="margin-bottom: 20px;">
                <button class="tab-btn ${timeFilter === 'upcoming' ? 'active' : ''}" data-tab="upcoming">Предстоящие события</button>
                <button class="tab-btn ${timeFilter === 'past' ? 'active' : ''}" data-tab="past">Прошедшие события</button>
            </div>

            <!-- Список событий -->
            ${events.length > 0 ? `
                <div class="grid grid-2">
                    ${events.map(event => {
            const distance = calculateEventDistance(event, currentUser);
            const distanceHtml = distance !== null ? `<div><strong>📏 Расстояние:</strong> ${distance.toFixed(1)} км</div>` : '';

            return `
                        <div class="card">
                            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px;">
                                <h3 style="margin: 0;">${escapeHtml(event.title)}</h3>
                                <div style="display: flex; align-items: center; gap: 8px;">
                                    ${event.is_past ? `
                                        <span class="badge badge-secondary">Завершено</span>
                                    ` : ''}
                                    <span class="badge badge-primary">
                                        ${getEventTypeLabel(event.type)}
                                    </span>
                                    ${event.is_private ? `
                                        <span class="badge badge-warning">🔒 Приватное</span>
                                    ` : ''}
                                </div>
                            </div>

                            <p style="color: var(--text-muted); margin-bottom: 15px;">${escapeHtml(event.description)}</p>

                            <div style="line-height: 1.8; margin-bottom: 20px;">
                                <div><strong>📅 Когда:</strong> ${formatDateTime(event.date_time)}</div>
                                <div><strong>📍 Где:</strong> ${escapeHtml(event.location)}</div>
                                <div><strong>👤 Организатор:</strong> ${escapeHtml(event.creator.first_name)} ${escapeHtml(event.creator.last_name)}</div>
                                ${event.max_participants ? `<div><strong>👥 Участников:</strong> до ${event.max_participants} человек</div>` : ''}
                                ${distanceHtml}

                                ${event.subscribers_count ? `
                                    <div style="margin-top: 10px; padding: 8px 12px; background: var(--light-bg); border-radius: 4px;">
                                        <strong>🔔 Подписчиков:</strong> ${event.subscribers_count}
                                    </div>
                                ` : ''}
                            </div>

                            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                ${event.is_private ? `
                                    <a href="/event/private/${event.private_key}" class="btn btn-outline btn-small">Подробнее</a>
                                ` : `
                                    <a href="/event/${event.id}" class="btn btn-outline btn-small">Подробнее</a>
                                `}

                                ${currentUser ? `
                                    ${event.is_past ? `
                                        <button class="btn btn-outline btn-small" disabled>Событие завершено</button>
                                    ` : `
                                        ${event.is_subscribed ? `
                                            <button class="btn btn-outline btn-small unsubscribe-btn" data-event-id="${event.id}">✅ Отписаться</button>
                                        ` : `
                                            <button class="btn btn-primary btn-small subscribe-btn" data-event-id="${event.id}">🔔 Подписаться</button>
                                        `}
                                    `}
                                ` : ''}

                                ${currentUser && currentUser.id === event.creator_id ? `
                                    <a href="/event/edit/${event.id}" class="btn btn-outline btn-small">Редактировать</a>
                                    <button class="btn btn-danger btn-small delete-btn" data-event-id="${event.id}">Удалить</button>
                                ` : ''}
                            </div>
                        </div>
                    `}).join('')}
                </div>
            ` : `
                <div class="empty-state">
                    ${timeFilter === 'past' ? `
                        <p>Нет прошедших событий.</p>
                    ` : `
                        <p>Пока нет событий.</p>
                    `}
                    <a href="/create-event" class="btn btn-primary">Создать первое событие</a>
                </div>
            `}

            <!-- Действия -->
            <div class="page-actions">
                <a href="/create-event" class="btn btn-success">Создать событие</a>
                ${currentUser ? `
                    <a href="/event-subscriptions" class="btn btn-outline">📋 Мои подписки</a>
                ` : ''}
            </div>
        `;

        // Добавляем обработчики событий
        setupEventHandlers();

    } catch (error) {
        console.error('Error loading events:', error);
        const eventsContent = document.getElementById('events-content');
        if (eventsContent) {
            eventsContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки событий: ${error.message}</div>
                <div class="page-actions">
                    <a href="/create-event" class="btn btn-success">Создать событие</a>
                </div>
            `;
        }
    }
});

// Вспомогательные функции
function getEventTypeLabel(type) {
    const typeLabels = {
        'concert': 'Концерт',
        'lecture': 'Лекция',
        'sport': 'Спорт',
        'meeting': 'Встреча',
        'party': 'Вечеринка',
        'conference': 'Конференция',
        'exhibition': 'Выставка',
        'other': 'Другое'
    };
    return typeLabels[type] || type;
}

function formatDateTime(dateTimeStr) {
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString('ru-RU') + ' ' + date.toLocaleTimeString('ru-RU', {hour: '2-digit', minute:'2-digit'});
}

function escapeHtml(unsafe) {
    if (!unsafe) return '';
    return unsafe
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function calculateEventDistance(event, currentUser) {
    if (!event.latitude || !event.longitude || !currentUser || !currentUser.latitude || !currentUser.longitude) {
        return null;
    }

    return calculateDistance(
        currentUser.latitude,
        currentUser.longitude,
        event.latitude,
        event.longitude
    );
}

function calculateDistance(lat1, lon1, lat2, lon2) {
    const R = 6371; // Радиус Земли в км
    const dLat = (lat2 - lat1) * Math.PI / 180;
    const dLon = (lon2 - lon1) * Math.PI / 180;
    const a =
        Math.sin(dLat/2) * Math.sin(dLat/2) +
        Math.cos(lat1 * Math.PI / 180) * Math.cos(lat2 * Math.PI / 180) *
        Math.sin(dLon/2) * Math.sin(dLon/2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
    const distance = R * c;
    return distance;
}

function setupEventHandlers() {
    // Обработчик формы приглашения
    const inviteForm = document.getElementById('invite-form');
    if (inviteForm) {
        inviteForm.addEventListener('submit', async function(e) {
            e.preventDefault();
            const code = document.getElementById('code').value.trim();
            if (code) {
                window.location.href = `/invite/${code}`;
            }
        });
    }

    // Обработчик фильтров
    const filterForm = document.getElementById('filter-form');
    if (filterForm) {
        filterForm.addEventListener('submit', function(e) {
            e.preventDefault();
            applyFilters();
        });
    }

    // Сброс фильтров
    const resetBtn = document.getElementById('reset-filters');
    if (resetBtn) {
        resetBtn.addEventListener('click', function() {
            window.location.href = `/events?filter=${document.querySelector('input[name="filter"]').value}`;
        });
    }

    // Переключение вкладок
    const tabButtons = document.querySelectorAll('.tab-btn');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const tab = this.getAttribute('data-tab');
            switchTab(tab);
        });
    });

    // Подписка/отписка
    document.addEventListener('click', async function(e) {
        if (e.target.classList.contains('subscribe-btn')) {
            const eventId = e.target.getAttribute('data-event-id');
            await subscribeToEvent(eventId);
        } else if (e.target.classList.contains('unsubscribe-btn')) {
            const eventId = e.target.getAttribute('data-event-id');
            await unsubscribeFromEvent(eventId);
        } else if (e.target.classList.contains('delete-btn')) {
            const eventId = e.target.getAttribute('data-event-id');
            await deleteEvent(eventId);
        }
    });
}

function applyFilters() {
    const form = document.getElementById('filter-form');
    const formData = new FormData(form);
    const params = new URLSearchParams();

    for (const [key, value] of formData) {
        if (value) params.append(key, value);
    }

    window.location.href = `/events?${params.toString()}`;
}

function switchTab(tab) {
    const urlParams = new URLSearchParams(window.location.search);
    urlParams.set('filter', tab);
    window.location.href = `/events?${urlParams.toString()}`;
}

async function subscribeToEvent(eventId) {
    try {
        const response = await fetch(`/api/event/${eventId}/subscribe`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Вы успешно подписались на событие', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка при подписке', 'error');
        }
    } catch (error) {
        console.error('Error subscribing to event:', error);
        showMessage('❌ Ошибка при подписке', 'error');
    }
}

async function unsubscribeFromEvent(eventId) {
    try {
        const response = await fetch(`/api/event/${eventId}/unsubscribe`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Вы отписались от события', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка при отписке', 'error');
        }
    } catch (error) {
        console.error('Error unsubscribing from event:', error);
        showMessage('❌ Ошибка при отписке', 'error');
    }
}

async function deleteEvent(eventId) {
    if (!confirm('Удалить событие?')) return;

    try {
        const response = await fetch(`/api/events/${eventId}/delete`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Событие удалено', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка при удалении', 'error');
        }
    } catch (error) {
        console.error('Error deleting event:', error);
        showMessage('❌ Ошибка при удалении', 'error');
    }
}

function showMessage(message, type) {
    // Создаем временное сообщение
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 1000;';
    alert.textContent = message;

    document.body.appendChild(alert);

    setTimeout(() => {
        alert.remove();
    }, 3000);
}
