document.addEventListener('DOMContentLoaded', async function() {
    const profilesContainer = document.getElementById('profiles-content');
    let allUsers = [];

    try {
        // Загружаем всех пользователей при загрузке страницы
        const response = await fetch('/api/profiles');
        const data = await response.json();

        if (!response.ok) {
            throw new Error(data.message || 'Ошибка загрузки пользователей');
        }

        allUsers = data.data || [];

        // Создаем основной контент
        profilesContainer.innerHTML = `
            <div class="card">
                <h2>Все пользователи (${allUsers.length})</h2>
                <div class="search-container" style="margin: 10px 0 20px;">
                    <input type="text" id="search-input" placeholder="Поиск по имени, email или телефону..." style="width: 100%; padding: 10px; border: 1px solid var(--border-color); border-radius: 4px;">
                </div>
                <div class="grid grid-3" id="profiles-grid">
                    ${renderUsers(allUsers)}
                </div>
                <div class="page-actions">
                    <a href="/profile" class="btn btn-outline">← Мой профиль</a>
                    <a href="/friends" class="btn btn-primary">Мои друзья</a>
                </div>
            </div>
        `;

        // Добавляем обработчик поиска
        const searchInput = document.getElementById('search-input');

        // Функция для выполнения поиска
        function performSearch() {
            const searchTerm = searchInput.value.toLowerCase().trim();
            const filteredUsers = searchTerm ?
                allUsers.filter(user =>
                    user.first_name.toLowerCase().includes(searchTerm) ||
                    user.last_name.toLowerCase().includes(searchTerm) ||
                    user.email.toLowerCase().includes(searchTerm) ||
                    (user.phone && user.phone.includes(searchTerm))
                ) :
                allUsers;

            const grid = document.getElementById('profiles-grid');
            if (grid) {
                grid.innerHTML = renderUsers(filteredUsers);
            }

            // Обновляем заголовок с количеством
            const header = profilesContainer.querySelector('.card h2');
            if (header) {
                header.textContent = `Все пользователи (${filteredUsers.length})`;
            }
        }

        // Добавляем debounce для оптимизации
        function debounce(func, wait) {
            let timeout;
            return function executedFunction(...args) {
                const later = () => {
                    clearTimeout(timeout);
                    func(...args);
                };
                clearTimeout(timeout);
                timeout = setTimeout(later, wait);
            };
        }

        // Назначаем обработчик с debounce
        searchInput.addEventListener('input', debounce(performSearch, 300));

    } catch (error) {
        console.error('Error loading profiles:', error);
        profilesContainer.innerHTML = `
            <div class="alert alert-error">Ошибка загрузки пользователей: ${error.message}</div>
        `;
    }

    // Функция для рендеринга пользователей
    function renderUsers(users) {
        if (users.length === 0) {
            return `
                <div class="empty-state">
                    <p>Пользователи не найдены.</p>
                </div>
            `;
        }

        return users.map(user => `
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
        `).join('');
    }
});