document.addEventListener('DOMContentLoaded', async function() {
    try {
        const profileContent = document.getElementById('profile-content');
        if (!profileContent) {
            console.error('Profile content element not found');
            return;
        }

        // Получаем ID профиля из атрибута данных или из window.profileId
        let profileId = profileContent.dataset.profileId || window.profileId;

        // Если мы получили флаг isOwnProfile из шаблона, используем его
        let isOwnProfile = window.isOwnProfile;

        console.log('Loading profile with ID:', profileId);
        console.log('Current user ID:', window.currentUser ? window.currentUser.id : 'none');

        // Формируем URL для запроса
        let url = '/api/profile';
        if (profileId && profileId !== 'undefined') {
            url = `/api/profile?id=${profileId}`;
        }

        // Получаем данные профиля через JSON API
        const response = await fetch(url, {
            headers: {
                'Accept': 'application/json',
            }
        });

        if (!response.ok) {
            if (response.status === 401) {
                window.location.href = '/login';
                return;
            }
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.error) {
            throw new Error(data.message || 'Ошибка загрузки профиля');
        }

        const user = data.user;
        const posts = data.posts || [];
        const followers = data.followers || 0;
        const following = data.following || 0;
        const friendsCount = data.friends_count || 0;
        const age = user.age || 0;
        const createdAt = user.created_at ? new Date(user.created_at).toLocaleDateString() : 'Не указана';
        const socialLinks = data.social_links || [];

        // Определяем, является ли профиль своим (если флаг не был установлен ранее)
        if (typeof isOwnProfile === 'undefined') {
            isOwnProfile = window.currentUser && window.currentUser.id === user.id;
        }

        console.log('Profile ownership:', { isOwnProfile, currentUserId: window.currentUser?.id, profileUserId: user.id });

        // Создаем HTML-содержимое
        profileContent.innerHTML = `
            <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
                <h2>${isOwnProfile ? 'Мой профиль' : user.first_name + ' ' + user.last_name}</h2>
                <span style="background: var(--primary-color); color: white; padding: 5px 10px; border-radius: 4px; font-size: 0.9em;">
                    👤 ${user.first_name} ${user.last_name}
                </span>
            </div>

            <!-- Статистика -->
            <div class="stats">
                <div class="stat-item">
                    <div class="stat-number">${followers}</div>
                    <div class="stat-label">Подписчиков</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">${following}</div>
                    <div class="stat-label">Подписок</div>
                </div>
                <div class="stat-item">
                    <div class="stat-number">${friendsCount}</div>
                    <div class="stat-label">Друзей</div>
                </div>
            </div>

            <!-- Информация профиля -->
            <div class="grid grid-2">
                <div>
                    <h3>Основная информация</h3>
                    <div style="line-height: 2;">
                        <div><strong>Email:</strong> ${user.email}</div>
                        <div><strong>Имя:</strong> ${user.first_name} ${user.last_name}</div>
                        <div><strong>Пол:</strong> ${user.gender === 'male' ? 'Мужской' : user.gender === 'female' ? 'Женский' : 'Не указан'}</div>
                        ${user.birth_date && user.birth_date !== '0001-01-01T00:00:00Z' ?
            `<div><strong>Дата рождения:</strong> ${new Date(user.birth_date).toLocaleDateString('ru-RU')}</div>
                             <div><strong>Возраст:</strong> ${calculateAge(user.birth_date)}</div>` :
            ''}
                        ${user.phone ? `<div><strong>Телефон:</strong> ${user.phone}</div>` : ''}
                        <div><strong>Зарегистрирован:</strong> ${createdAt}</div>
                    </div>
                </div>
            </div>

            <!-- Социальные сети -->
            ${socialLinks.length > 0 ? `
            <div class="social-links">
                <h3>Социальные сети</h3>
                <div class="grid grid-3" style="gap: 15px; margin-top: 10px;">
                    ${socialLinks.map(link => `
                        <a href="${getPlatformUrl(link.platform, link.username)}" target="_blank" style="display: flex; align-items: center; gap: 8px; text-decoration: none; color: var(--text-color);">
                            <span style="font-size: 1.2em;">${getPlatformIcon(link.platform)}</span>
                            <span>${getPlatformDisplayName(link.platform, link.custom_name)}</span>
                            <span style="color: var(--text-muted); font-size: 0.9em;">@${link.username}</span>
                        </a>
                    `).join('')}
                </div>
            </div>
            ` : ''}

            <!-- Стена пользователя -->
            <h3>Стена</h3>
            
            ${isOwnProfile ? `
            <!-- Форма новой записи (только для своего профиля) -->
            <div style="background: var(--light-bg); padding: 20px; border-radius: 8px; margin-bottom: 20px;">
                <form id="wall-post-form">
                    <textarea name="content" class="form-control" placeholder="Что у вас нового?" required maxlength="1000" rows="4"></textarea>
                    <input type="hidden" name="user_id" value="${user.id}">
                    <button type="submit" class="btn btn-primary" style="margin-top: 10px;">Опубликовать</button>
                </form>
            </div>
            ` : ''}

            <!-- Список записей -->
            <div class="posts">
                ${posts.length > 0 ?
            posts.map(post => `
                    <div class="post">
                        <div class="post-header">
                            <span class="post-author">${post.author.first_name} ${post.author.last_name}</span>
                            <span class="post-date">${new Date(post.created_at).toLocaleDateString() + ' ' + new Date(post.created_at).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
                        </div>
                        <div class="post-content">${post.content}</div>
                        ${isOwnProfile ? `
                        <div class="post-actions">
                            <a href="/wall/edit/${post.id}" class="btn btn-outline btn-small">Редактировать</a>
                            <a href="/wall/delete/${post.id}" class="btn btn-danger btn-small" onclick="return confirm('Удалить запись?')">Удалить</a>
                        </div>
                        ` : ''}
                    </div>
                    `).join('')
            :
            `<div class="empty-state">
                        <p>На стене пока нет записей.</p>
                        ${isOwnProfile ? '<p>Напишите что-нибудь первым!</p>' : ''}
                    </div>`
        }
            </div>

            <!-- Действия профиля -->
            ${isOwnProfile ? `
            <div class="page-actions">
                <a href="/edit-profile" class="btn btn-primary">Редактировать профиль</a>
                <a href="/friends" class="btn btn-outline">Мои друзья</a>
                <a href="/subscriptions" class="btn btn-outline">Мои подписки</a>
                <a href="/create-event" class="btn btn-success">Создать событие</a>
            </div>
            ` : `
            <div class="page-actions">
                <a href="/profiles" class="btn btn-outline">← Все пользователи</a>
                ${window.currentUser && window.currentUser.id !== user.id ? `
                    <button class="btn btn-primary" onclick="subscribeToUser(${user.id})">Подписаться</button>
                    <button class="btn btn-success" onclick="addFriend(${user.id})">Добавить в друзья</button>
                ` : ''}
            </div>
            `}
        `;

        // Добавляем обработчик формы
        if (isOwnProfile) {
            const form = document.getElementById('wall-post-form');
            if (form) {
                form.addEventListener('submit', handleWallPost);
            }
        }

    } catch (error) {
        console.error('Error loading profile:', error);
        const profileContent = document.getElementById('profile-content');
        if (profileContent) {
            profileContent.innerHTML = `
                <div class="alert alert-error">
                    Ошибка загрузки профиля: ${error.message}
                    <br><br>
                    <a href="/profiles" class="btn btn-outline">Вернуться к списку пользователей</a>
                </div>
            `;
        }
    }
});

// Вспомогательные функции (остаются без изменений)
function getPlatformDisplayName(platform, customName) {
    const platformNames = {
        'vk': 'VKонтакте',
        'tg': 'Telegram',
        'custom': customName || 'Другая сеть'
    };
    return platformNames[platform] || platform;
}

function getPlatformIcon(platform) {
    const icons = {
        'vk': '📱',
        'tg': '📱',
        'custom': '🔗'
    };
    return icons[platform] || '🔗';
}

function getPlatformUrl(platform, username) {
    if (!username) return '#';

    const urls = {
        'vk': `https://vk.com/${username}`,
        'tg': `https://t.me/${username}`
    };

    return urls[platform] || '#';
}

async function handleWallPost(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const content = formData.get('content');
    const userId = formData.get('user_id');

    try {
        const response = await fetch('/api/wall/posts', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({
                content: content,
                user_id: parseInt(userId)
            })
        });

        const result = await response.json();

        if (result.success) {
            event.target.reset();
            window.location.reload();
        } else {
            alert('Ошибка: ' + result.message);
        }
    } catch (error) {
        console.error('Error posting to wall:', error);
        alert('Ошибка при публикации записи');
    }
}

async function subscribeToUser(userId) {
    try {
        const response = await fetch(`/api/friends/subscribe/${userId}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            alert('Вы успешно подписались на пользователя');
            window.location.reload();
        } else {
            alert('Ошибка: ' + result.message);
        }
    } catch (error) {
        console.error('Error subscribing:', error);
        alert('Ошибка при подписке');
    }
}

async function addFriend(userId) {
    try {
        const response = await fetch(`/api/friends/add/${userId}`, {
            method: 'POST'
        });

        const result = await response.json();

        if (result.success) {
            alert('Запрос на добавление в друзья отправлен');
            window.location.reload();
        } else {
            alert('Ошибка: ' + result.message);
        }
    } catch (error) {
        console.error('Error adding friend:', error);
        alert('Ошибка при отправке запроса');
    }
}

function calculateAge(birthDate) {
    if (!birthDate || birthDate === '0001-01-01T00:00:00Z') return 0;

    const birth = new Date(birthDate);
    const today = new Date();
    let age = today.getFullYear() - birth.getFullYear();
    const monthDiff = today.getMonth() - birth.getMonth();

    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birth.getDate())) {
        age--;
    }

    return age;
}