document.addEventListener('DOMContentLoaded', async function() {
    try {
        // Получаем список пользователей через JSON API
        const response = await fetch('/api/profiles');
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Ошибка загрузки пользователей');
        }

        const users = data.data || [];
        const currentUser = window.currentUser;

        const container = document.getElementById('profiles-content');
        container.innerHTML = '';

        // Создаем заголовок
        const header = document.createElement('h2');
        header.textContent = `Все пользователи (${users.length})`;
        container.appendChild(header);

        if (users.length === 0) {
            container.innerHTML += `
                <div class="empty-state">
                    <p>Пока нет пользователей.</p>
                    <a href="/create-profile" class="btn btn-primary">Создать первый профиль</a>
                </div>
            `;
            return;
        }

        // Создаем сетку для пользователей
        const grid = document.createElement('div');
        grid.className = 'grid grid-3';

        users.forEach(user => {
            const card = document.createElement('div');
            card.className = 'card';

            // Определяем статус дружбы и подписки (упрощенно)
            let friendshipStatus = 'none';
            let isSubscribed = false;

            if (currentUser && currentUser.id !== user.id) {
                friendshipStatus = user.friendship_status || 'none';
                isSubscribed = user.is_subscribed || false;
            }

            card.innerHTML = `
                <h4>${user.first_name} ${user.last_name}</h4>
                <p style="color: var(--text-muted); margin: 10px 0;">${user.email}</p>
                ${user.age ? `<p>Возраст: ${user.age} лет</p>` : ''}

                <!-- Статистика -->
                <div style="font-size: 0.9em; color: var(--text-muted); margin: 15px 0; padding: 10px; background: var(--light-bg); border-radius: 4px;">
                    <div>Подписчиков: ${user.followers_count || 0}</div>
                    <div>Подписок: ${user.following_count || 0}</div>
                </div>

                <!-- Социальные сети -->
                ${user.social_links && user.social_links.length > 0 ? `
                <div class="social-links" style="margin: 10px 0;">
                    ${user.social_links.slice(0, 2).map(link => `
                        <div style="display: flex; align-items: center; gap: 6px; font-size: 0.9em; color: var(--text-muted);">
                            <span>${link.platform === 'vk' ? '📱' : link.platform === 'tg' ? '📱' : '🔗'}</span>
                            <span>${link.platform}</span>
                            <span>@${link.username}</span>
                        </div>
                    `).join('')}
                    ${user.social_links.length > 2 ? `<div style="font-size: 0.8em; color: var(--text-muted);">+${user.social_links.length - 2} еще</div>` : ''}
                </div>
                ` : ''}



                <!-- Действия -->
                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                    <a href="/profile/${user.id}" class="btn btn-outline btn-small">Профиль</a>
                    ${currentUser && currentUser.id !== user.id ? `
                        ${isSubscribed ? 
                            `<a href="/unsubscribe/${user.id}" class="btn btn-secondary btn-small">Отписаться</a>` :
                            `<a href="/subscribe/${user.id}" class="btn btn-primary btn-small">Подписаться</a>`
                        }

                        ${friendshipStatus === 'none' ? 
                            `<a href="/friends/add/${user.id}" class="btn btn-success btn-small">Добавить в друзья</a>` :
                            friendshipStatus === 'pending' ? 
                                `<span class="badge badge-pending">Запрос отправлен</span>` :
                                friendshipStatus === 'accepted' ? 
                                    `<span class="badge badge-success">В друзьях</span>` : ''
                        }
                    ` : ''}
                </div>
            `;

            grid.appendChild(card);
        });

        container.appendChild(grid);

        // Добавляем кнопки действий
        container.innerHTML += `
            <div class="page-actions">
                <a href="/profile" class="btn btn-outline">← Мой профиль</a>
                <a href="/friends" class="btn btn-primary">Мои друзья</a>
            </div>
        `;

    } catch (error) {
        console.error('Error loading profiles:', error);
        document.getElementById('profiles-content').innerHTML = `
            <div class="alert alert-error">Ошибка загрузки пользователей: ${error.message}</div>
        `;
    }
});