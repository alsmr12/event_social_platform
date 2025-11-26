document.addEventListener('DOMContentLoaded', async function() {
    try {
        const response = await fetch('/api/profiles');
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Ошибка загрузки пользователей');
        }

        const users = data.data || [];
        const container = document.getElementById('profiles-content');

        container.innerHTML = `
            <div class="card">
                <h2>Все пользователи (${users.length})</h2>

                ${users.length > 0 ? `
                <div class="grid grid-3">
                    ${users.map(user => `
                        <div class="card">
                            <h4>${user.first_name} ${user.last_name}</h4>
                            <p style="color: var(--text-muted); margin: 10px 0;">${user.email}</p>
                            ${user.age ? `<p>Возраст: ${user.age} ${user.age === 1 || (user.age % 10 === 1 && user.age % 100 !== 11) ? 'год' : (user.age >= 2 && user.age <= 4 || (user.age % 10 >= 2 && user.age % 10 <= 4 && !(user.age % 100 >= 12 && user.age % 100 <= 14))) ? 'года' : 'лет'}</p>` : ''}

                            <!-- Статистика -->
                            <div style="font-size: 0.9em; color: var(--text-muted); margin: 15px 0; padding: 10px; background: var(--light-bg); border-radius: 4px;">
                                <div>Подписчиков: ${user.followers_count || 0}</div>
                                <div>Подписок: ${user.following_count || 0}</div>
                                <div>Друзей: ${user.friends_count || 0}</div>
                            </div>

                            <!-- Действия -->
                            <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                <a href="/profile/${user.id}" class="btn btn-outline btn-small">Профиль</a>
                            </div>
                        </div>
                    `).join('')}
                </div>
                ` : `
                <div class="empty-state">
                    <p>Пока нет пользователей.</p>
                    <a href="/create-profile" class="btn btn-primary">Создать первый профиль</a>
                </div>
                `}

                <div class="page-actions">
                    <a href="/profile" class="btn btn-outline">← Мой профиль</a>
                    <a href="/friends" class="btn btn-primary">Мои друзья</a>
                </div>
            </div>
        `;

    } catch (error) {
        console.error('Error loading profiles:', error);
        document.getElementById('profiles-content').innerHTML = `
            <div class="alert alert-error">Ошибка загрузки пользователей: ${error.message}</div>
        `;
    }
});