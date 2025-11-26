
document.addEventListener('DOMContentLoaded', async function() {
    try {
        const eventContent = document.getElementById('event-content');
        if (!eventContent) {
            console.error('Event content element not found');
            return;
        }

        // Получаем ID события или приватный ключ из data-атрибутов
        const eventId = eventContent.dataset.eventId;
        const privateKey = eventContent.dataset.privateKey;

        console.log('Loading event:', { eventId, privateKey });

        let apiUrl;
        if (privateKey && privateKey !== '') {
            apiUrl = `/api/event/private/${privateKey}`;
        } else if (eventId && eventId !== '') {
            apiUrl = `/api/event/${eventId}`;
        } else {
            throw new Error('Event ID or private key not found');
        }

        console.log('Fetching from:', apiUrl);

        // Получаем данные события
        const response = await fetch(apiUrl);

        if (!response.ok) {
            if (response.status === 404) {
                throw new Error('Событие не найдено');
            } else if (response.status === 403) {
                throw new Error('У вас нет доступа к этому приватному событию');
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Ошибка загрузки события');
        }

        const event = data.event;
        const currentUser = window.currentUser;

        // Создаем HTML-содержимое
        eventContent.innerHTML = createEventHTML(event, currentUser);

        // Добавляем обработчики событий
        setupEventHandlers(event);

    } catch (error) {
        console.error('Error loading event:', error);
        const eventContent = document.getElementById('event-content');
        if (eventContent) {
            eventContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки события: ${error.message}</div>
                <div class="page-actions">
                    <a href="/events" class="btn btn-outline">← Все события</a>
                </div>
            `;
        }
    }
});

function createEventHTML(event, currentUser) {
    const baseURL = window.location.origin;
    const distance = calculateEventDistance(event, currentUser);
    const distanceHtml = distance !== null ? `<div><strong>📏 Расстояние:</strong> ${distance.toFixed(1)} км</div>` : '';

    return `
        ${event.is_private ? `
            <div class="private-event-info" style="background: #f0f8ff; padding: 15px; border-radius: 8px; margin-bottom: 20px; border-left: 4px solid #007bff;">
                <h4 style="margin: 0 0 10px 0; color: #007bff;">
                    <i class="fas fa-lock"></i> Приватное событие
                </h4>
                <p style="margin: 0 0 10px 0; color: #666;">
                    Это приватное событие. Поделитесь ссылкой-приглашением для доступа:
                </p>
                <div class="invite-link" style="display: flex; gap: 10px; align-items: center;">
                    <input type="text" 
                           value="${baseURL}/event/private/${event.private_key}" 
                           readonly 
                           id="inviteLink"
                           style="flex: 1; padding: 8px 12px; border: 1px solid #ddd; border-radius: 4px; background: #f9f9f9; font-family: monospace;">
                    <button onclick="copyInviteLink()" 
                            style="padding: 8px 16px; background: #007bff; color: white; border: none; border-radius: 4px; cursor: pointer; white-space: nowrap;">
                        Копировать ссылку
                    </button>
                </div>
                ${event.invite_code ? `
                    <p style="margin: 10px 0 0 0; font-size: 0.9em; color: #666;">
                        Код приглашения: <code style="background: #e9ecef; padding: 2px 6px; border-radius: 3px;">${event.invite_code}</code>
                    </p>
                ` : ''}
            </div>
        ` : ''}

        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 20px;">
            <div>
                <h2>${event.title}</h2>
                <div style="display: flex; gap: 8px; align-items: center; margin-top: 8px;">
                    <span class="badge badge-primary">
                        ${getEventTypeLabel(event.type)}
                    </span>
                    ${event.is_private ? `<span class="badge badge-warning">🔒 Приватное</span>` : ''}
                    ${event.is_past ? `<span class="badge badge-secondary">Завершено</span>` : ''}
                </div>
            </div>
            <div style="display: flex; gap: 8px; align-items: center;">
                ${currentUser ? `
                    ${event.is_past ? `
                        <button class="btn btn-outline btn-small" disabled>Событие завершено</button>
                    ` : `
                        ${event.is_subscribed ? `
                            <button class="btn btn-outline btn-small unsubscribe-btn">✅ Отписаться</button>
                        ` : `
                            <button class="btn btn-primary btn-small subscribe-btn">🔔 Подписаться</button>
                        `}
                    `}
                ` : ''}

                ${currentUser && currentUser.id === event.creator_id ? `
                    <div style="display: flex; gap: 8px;">
                        <a href="/event/edit/${event.id}" class="btn btn-outline btn-small">Редактировать</a>
                        <button class="btn btn-danger btn-small delete-btn">Удалить</button>
                    </div>
                ` : ''}
            </div>
        </div>

        <div class="grid grid-2">
            <div>
                <h3>Описание</h3>
                <p style="line-height: 1.6;">${event.description}</p>

                ${currentUser ? `
                    <div style="margin-top: 20px; padding: 15px; background: var(--light-bg); border-radius: 4px;">
                        <h4>📋 Ваше участие</h4>
                        ${event.is_subscribed ? `
                            <p>✅ <strong>Вы подписаны на это событие</strong></p>
                            <p><small>Подписка оформлена.</small></p>
                        ` : `
                            <p>❌ <strong>Вы не подписаны на это событие</strong></p>
                            <p><small>Подпишитесь!</small></p>
                        `}
                    </div>
                ` : ''}

                ${event.subscribers_count ? `
                    <div style="margin-top: 20px; padding: 15px; background: var(--light-bg); border-radius: 4px;">
                        <h4>🔔 Подписчики</h4>
                        <p>На это событие подписано: <strong>${event.subscribers_count}</strong> человек</p>
                    </div>
                ` : ''}
            </div>

            <div>
                <h3>Информация</h3>
                <div style="line-height: 2;">
                    <div><strong>📅 Дата и время:</strong> ${formatDateTime(event.date_time)}</div>
                    <div><strong>📍 Место:</strong> ${event.location}</div>
                    ${event.latitude ? `<div><strong>🌐 Координаты:</strong> ${event.latitude}, ${event.longitude}</div>` : ''}
                    ${distanceHtml}
                    <div><strong>👤 Организатор:</strong> ${event.creator.first_name} ${event.creator.last_name}</div>
                    ${event.max_participants ? `<div><strong>👥 Макс. участников:</strong> ${event.max_participants}</div>` : ''}
                    <div><strong>📅 Создано:</strong> ${formatDate(event.created_at)}</div>
                    ${event.is_past ? `<div><strong>📊 Статус:</strong> <span style="color: #6c757d;">Завершено</span></div>` : ''}
                    ${event.is_private ? `
                        <div><strong>🔐 Доступ:</strong> <span style="color: #e9c46a;">Приватное (только по ссылке-приглашению)</span></div>
                        ${event.invite_code ? `
                            <div><strong>🔑 Код приглашения:</strong> <code style="background: #f8f9fa; padding: 2px 6px; border-radius: 3px;">${event.invite_code}</code></div>
                        ` : ''}
                    ` : ''}
                </div>
            </div>
        </div>

        <div class="page-actions">
            <a href="/events" class="btn btn-outline">← Все события</a>
            ${currentUser ? `
                <a href="/event-subscriptions" class="btn btn-outline">📋 Мои подписки</a>
            ` : ''}
            <a href="/create-event" class="btn btn-success">Создать событие</a>
        </div>
    `;
}

function setupEventHandlers(event) {
    // Подписка/отписка
    const subscribeBtn = document.querySelector('.subscribe-btn');
    const unsubscribeBtn = document.querySelector('.unsubscribe-btn');
    const deleteBtn = document.querySelector('.delete-btn');

    if (subscribeBtn) {
        subscribeBtn.addEventListener('click', async () => {
            await subscribeToEvent(event.id);
        });
    }

    if (unsubscribeBtn) {
        unsubscribeBtn.addEventListener('click', async () => {
            await unsubscribeFromEvent(event.id);
        });
    }

    if (deleteBtn) {
        deleteBtn.addEventListener('click', async () => {
            await deleteEvent(event.id);
        });
    }
}

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

function formatDate(dateStr) {
    return new Date(dateStr).toLocaleDateString('ru-RU');
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

function copyInviteLink() {
    const link = document.getElementById('inviteLink');
    link.select();
    link.setSelectionRange(0, 99999);
    document.execCommand('copy');

    const button = event.target;
    const originalText = button.textContent;
    button.textContent = 'Скопировано!';
    button.style.background = '#28a745';

    setTimeout(() => {
        button.textContent = originalText;
        button.style.background = '#007bff';
    }, 2000);
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
            setTimeout(() => window.location.href = '/events', 1000);
        } else {
            showMessage(data.message || '❌ Ошибка при удалении', 'error');
        }
    } catch (error) {
        console.error('Error deleting event:', error);
        showMessage('❌ Ошибка при удалении', 'error');
    }
}

function showMessage(message, type) {
    const alert = document.createElement('div');
    alert.className = `alert alert-${type}`;
    alert.style.cssText = 'position: fixed; top: 20px; right: 20px; z-index: 1000;';
    alert.textContent = message;

    document.body.appendChild(alert);

    setTimeout(() => {
        alert.remove();
    }, 3000);
}
