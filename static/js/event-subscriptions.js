document.addEventListener('DOMContentLoaded', async function() {
    try {
        const subscriptionsContent = document.getElementById('event-subscriptions-content');
        if (!subscriptionsContent) {
            console.error('Event subscriptions content element not found');
            return;
        }

        // Получаем параметр фильтра из URL
        const urlParams = new URLSearchParams(window.location.search);
        const filter = urlParams.get('filter') || 'upcoming';

        // Получаем данные подписок
        const response = await fetch(`/api/event-subscriptions?filter=${filter}`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Ошибка загрузки подписок');
        }

        const subscriptions = data.subscriptions || [];
        const currentUser = window.currentUser;

        // Создаем HTML-содержимое
        subscriptionsContent.innerHTML = createSubscriptionsHTML(subscriptions, filter, currentUser);

        // Добавляем обработчики событий
        setupSubscriptionsHandlers();

    } catch (error) {
        console.error('Error loading event subscriptions:', error);
        const subscriptionsContent = document.getElementById('event-subscriptions-content');
        if (subscriptionsContent) {
            subscriptionsContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки подписок: ${error.message}</div>
                <div class="page-actions">
                    <a href="/events" class="btn btn-outline">← Все события</a>
                </div>
            `;
        }
    }
});

function createSubscriptionsHTML(subscriptions, filter, currentUser) {
    const isPast = filter === 'past';

    return `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
            <h2>Мои подписки на события</h2>
        </div>

        <!-- Вкладки -->
        <div class="tabs" style="margin-bottom: 20px;">
            <button class="btn ${!isPast ? 'btn-primary' : 'btn-outline'}" data-tab="upcoming">Предстоящие события</button>
            <button class="btn ${isPast ? 'btn-primary' : 'btn-outline'}" data-tab="past">Прошедшие события</button>
        </div>

        ${subscriptions.length > 0 ? `
            <div class="grid grid-2">
                ${subscriptions.map(subscription => {
        const event = subscription.event;
        const distance = calculateEventDistance(event, currentUser);
        const distanceHtml = distance !== null ? `<div><strong>📏 Расстояние:</strong> ${distance.toFixed(1)} км</div>` : '';

        return `
                        <div class="card">
                            <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 15px;">
                                <h3 style="margin: 0;">${event.title}</h3>
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

                            <p style="color: var(--text-muted); margin-bottom: 15px;">${event.description}</p>

                            <div style="line-height: 1.8; margin-bottom: 20px;">
                                <div><strong>📅 Когда:</strong> ${formatDateTime(event.date_time)}</div>
                                <div><strong>📍 Где:</strong> ${event.location}</div>
                                <div><strong>👤 Организатор:</strong> ${event.creator.first_name} ${event.creator.last_name}</div>
                                ${event.max_participants ? `<div><strong>👥 Участников:</strong> до ${event.max_participants} человек</div>` : ''}
                                ${distanceHtml}
                                <div><strong>📅 Подписан:</strong> ${formatDate(subscription.created_at)}</div>

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

                                ${event.is_past ? `
                                    <button class="btn btn-outline btn-small" disabled>Событие завершено</button>
                                ` : `
                                    <button class="btn btn-danger btn-small unsubscribe-btn" data-event-id="${event.id}">Отписаться</button>
                                `}
                            </div>
                        </div>
                    `;
    }).join('')}
            </div>
        ` : `
            <div class="empty-state">
                ${isPast ? `
                    <p>У вас нет подписок на прошедшие события.</p>
                ` : `
                    <p>Вы еще не подписаны ни на одно событие.</p>
                `}
                <a href="/events" class="btn btn-primary">Найти события</a>
            </div>
        `}

        <div class="page-actions">
            <a href="/events" class="btn btn-outline">← Все события</a>
            <a href="/create-event" class="btn btn-success">Создать событие</a>
        </div>
    `;
}

function setupSubscriptionsHandlers() {
    // Переключение вкладок
    const tabButtons = document.querySelectorAll('.tabs .btn');
    tabButtons.forEach(btn => {
        btn.addEventListener('click', function() {
            const tab = this.getAttribute('data-tab');
            switchTab(tab);
        });
    });

    // Отписка от событий
    document.addEventListener('click', async function(e) {
        if (e.target.classList.contains('unsubscribe-btn')) {
            const eventId = e.target.getAttribute('data-event-id');
            await unsubscribeFromEvent(eventId);
        }
    });
}

function switchTab(tab) {
    window.location.href = `/event-subscriptions?filter=${tab}`;
}

async function unsubscribeFromEvent(eventId) {
    if (!confirm('Отписаться от события?')) return;

    try {
        const response = await fetch(`/api/event-subscriptions/${eventId}/unsubscribe`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            }
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Вы отписались от события', 'success');
            // Находим кнопку по data-event-id и удаляем карточку
            const button = document.querySelector(`[data-event-id="${eventId}"]`);
            if (button) {
                const card = button.closest('.card');
                if (card) {
                    card.remove();
                }
            }
        } else {
            showMessage(data.message || '❌ Ошибка при отписке', 'error');
        }
    } catch (error) {
        console.error('Error unsubscribing from event:', error);
        showMessage('❌ Ошибка при отписке', 'error');
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