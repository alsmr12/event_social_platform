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
                <div style="display: flex; flex-direction: column; gap: 16px;">
                    ${users.map(user => `
                        <div class="card" style="padding: 20px;">
                            <div style="display: flex; align-items: flex-start; gap: 16px;">
                                <!-- Аватар -->
                                <div style="
                                    width: 48px; 
                                    height: 48px; 
                                    border-radius: 8px; 
                                    background: var(--light-bg);
                                    display: flex; 
                                    align-items: center; 
                                    justify-content: center; 
                                    color: var(--text-muted); 
                                    font-weight: bold; 
                                    font-size: 1.1em;
                                    flex-shrink: 0;
                                    border: 1px solid var(--border-color);
                                ">
                                    ${user.first_name ? user.first_name[0].toUpperCase() : 'U'}
                                </div>
                                
                                <!-- Основная информация -->
                                <div style="flex: 1;">
                                    <div style="display: flex; align-items: center; gap: 12px; margin-bottom: 8px;">
                                        <h4 style="margin: 0; color: var(--text-color);">${user.first_name} ${user.last_name}</h4>
                                        ${user.age ? `<span style="color: var(--text-muted); font-size: 0.9em;">${user.age} лет</span>` : ''}
                                    </div>
                                    
                                    <p style="color: var(--text-muted); margin: 0 0 16px 0;">${user.email}</p>
                                    
                                    <!-- Статистика -->
                                    <div style="display: flex; gap: 24px;">
                                        <div style="text-align: center;">
                                            <div style="font-size: 1.2em; font-weight: 600; color: var(--text-color);">${user.followers_count || 0}</div>
                                            <div style="font-size: 0.8em; color: var(--text-muted);">Подписчиков</div>
                                        </div>
                                        <div style="text-align: center;">
                                            <div style="font-size: 1.2em; font-weight: 600; color: var(--text-color);">${user.following_count || 0}</div>
                                            <div style="font-size: 0.8em; color: var(--text-muted);">Подписок</div>
                                        </div>
                                        <div style="text-align: center;">
                                            <div style="font-size: 1.2em; font-weight: 600; color: var(--text-color);">${user.friends_count || 0}</div>
                                            <div style="font-size: 0.8em; color: var(--text-muted);">Друзей</div>
                                        </div>
                                    </div>
                                </div>
                                
                                <!-- Кнопка -->
                                <div style="flex-shrink: 0;">
                                    <a href="/profile/${user.id}" class="btn btn-outline btn-small">Профиль</a>
                                </div>
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