
document.addEventListener('DOMContentLoaded', async function() {
    try {
        const friendsContent = document.getElementById('friends-content');
        if (!friendsContent) {
            console.error('Friends content element not found');
            return;
        }

        // Загружаем данные друзей
        const [friendsRes, pendingRes, sentRes] = await Promise.all([
            fetch('/api/friends'),
            fetch('/api/friends/pending'),
            fetch('/api/friends/sent')
        ]);

        const friendsData = await friendsRes.json();
        const pendingData = await pendingRes.json();
        const sentData = await sentRes.json();

        if (!friendsData.success || !pendingData.success || !sentData.success) {
            throw new Error('Ошибка загрузки данных друзей');
        }

        const friends = friendsData.friends || [];
        const pendingRequests = pendingData.requests || [];
        const sentRequests = sentData.requests || [];
        const currentUser = window.currentUser;

        // Создаем HTML
        friendsContent.innerHTML = `
            <h2>Мои друзья</h2>

            <!-- Входящие запросы -->
            <div style="margin-bottom: 40px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h3>Входящие запросы</h3>
                    <span class="badge badge-pending">${pendingRequests.length}</span>
                </div>

                ${pendingRequests.length > 0 ? `
                    <div class="grid grid-3">
                        ${pendingRequests.map(request => `
                            <div class="card">
                                <h4>${request.user.first_name} ${request.user.last_name}</h4>
                                <p style="color: var(--text-muted); margin: 10px 0;">${request.user.email}</p>
                                
                                <div style="font-size: 0.85em; color: var(--text-muted); margin: 10px 0;">
                                    Запрос отправлен: ${formatDateTime(request.created_at)}
                                </div>
                                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                    <button class="btn btn-success btn-small accept-btn" data-user-id="${request.user.id}">Принять</button>
                                    <button class="btn btn-danger btn-small reject-btn" data-user-id="${request.user.id}">Отклонить</button>
                                    <a href="/profile/${request.user.id}" class="btn btn-outline btn-small">Профиль</a>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : `
                    <div class="empty-state">
                        <p>Нет входящих запросов в друзья</p>
                    </div>
                `}
            </div>

            <!-- Исходящие запросы -->
            <div style="margin-bottom: 40px;">
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h3>Исходящие запросы</h3>
                    <span class="badge badge-pending">${sentRequests.length}</span>
                </div>

                ${sentRequests.length > 0 ? `
                    <div class="grid grid-3">
                        ${sentRequests.map(request => `
                            <div class="card">
                                <h4>${request.friend.first_name} ${request.friend.last_name}</h4>
                                <p style="color: var(--text-muted); margin: 10px 0;">${request.friend.email}</p>
                                
                                <div style="font-size: 0.85em; color: var(--text-muted); margin: 10px 0;">
                                    Запрос отправлен: ${formatDateTime(request.created_at)}
                                </div>
                                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                    <a href="/profile/${request.friend.id}" class="btn btn-outline btn-small">Профиль</a>
                                    <button class="btn btn-secondary btn-small cancel-btn" data-user-id="${request.friend.id}">Отменить</button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : `
                    <div class="empty-state">
                        <p>Нет исходящих запросов в друзья</p>
                    </div>
                `}
            </div>

            <!-- Список друзей -->
            <div>
                <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px;">
                    <h3>Мои друзья</h3>
                    <span class="badge badge-success">${friends.length}</span>
                </div>

                ${friends.length > 0 ? `
                    <div class="grid grid-3">
                        ${friends.map(friend => `
                            <div class="card">
                                <h4>${friend.first_name} ${friend.last_name}</h4>
                                <p style="color: var(--text-muted); margin: 10px 0;">${friend.email}</p>
                                
                                <div style="display: flex; gap: 8px; flex-wrap: wrap;">
                                    <a href="/profile/${friend.id}" class="btn btn-outline btn-small">Профиль</a>
                                    <button class="btn btn-danger btn-small remove-btn" data-user-id="${friend.id}">Удалить</button>
                                </div>
                            </div>
                        `).join('')}
                    </div>
                ` : `
                    <div class="empty-state">
                        <p>У вас пока нет друзей</p>
                        <a href="/profiles" class="btn btn-primary" style="margin-top: 15px;">Найти друзей</a>
                    </div>
                `}
            </div>

            <div class="page-actions">
                <a href="/profiles" class="btn btn-primary">Найти новых друзей</a>
                <a href="/profile" class="btn btn-outline">← Мой профиль</a>
            </div>
        `;

        // Добавляем обработчики
        setupFriendsHandlers();

    } catch (error) {
        console.error('Error loading friends:', error);
        const friendsContent = document.getElementById('friends-content');
        if (friendsContent) {
            friendsContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки друзей: ${error.message}</div>
                <div class="page-actions">
                    <a href="/profile" class="btn btn-outline">← Мой профиль</a>
                </div>
            `;
        }
    }
});

function setupFriendsHandlers() {
    // Принять запрос в друзья
    document.addEventListener('click', async function(e) {
        if (e.target.classList.contains('accept-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            await acceptFriendRequest(userId);
        }
        // Отклонить запрос в друзья
        else if (e.target.classList.contains('reject-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            await rejectFriendRequest(userId);
        }
        // Отменить исходящий запрос
        else if (e.target.classList.contains('cancel-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            await cancelFriendRequest(userId);
        }
        // Удалить друга
        else if (e.target.classList.contains('remove-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            await removeFriend(userId);
        }
    });
}

async function acceptFriendRequest(userId) {
    try {
        const response = await fetch(`/api/friends/accept/${userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Запрос в друзья принят', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка', 'error');
        }
    } catch (error) {
        console.error('Error accepting friend request:', error);
        showMessage('❌ Ошибка при принятии запроса', 'error');
    }
}

async function rejectFriendRequest(userId) {
    if (!confirm('Отклонить запрос в друзья?')) return;

    try {
        const response = await fetch(`/api/friends/reject/${userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Запрос в друзья отклонен', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка', 'error');
        }
    } catch (error) {
        console.error('Error rejecting friend request:', error);
        showMessage('❌ Ошибка при отклонении запроса', 'error');
    }
}

async function cancelFriendRequest(userId) {
    if (!confirm('Отменить запрос в друзья?')) return;

    try {
        const response = await fetch(`/api/friends/remove/${userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Запрос отменен', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка', 'error');
        }
    } catch (error) {
        console.error('Error canceling friend request:', error);
        showMessage('❌ Ошибка при отмене запроса', 'error');
    }
}

async function removeFriend(userId) {
    if (!confirm('Удалить из друзей?')) return;

    try {
        const response = await fetch(`/api/friends/remove/${userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Пользователь удален из друзей', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка', 'error');
        }
    } catch (error) {
        console.error('Error removing friend:', error);
        showMessage('❌ Ошибка при удалении друга', 'error');
    }
}

// Вспомогательные функции
function calculateAge(birthDateStr) {
    const birthDate = new Date(birthDateStr);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }
    return age;
}

function formatDateTime(dateTimeStr) {
    const date = new Date(dateTimeStr);
    return date.toLocaleDateString('ru-RU') + ' ' + date.toLocaleTimeString('ru-RU', {hour: '2-digit', minute:'2-digit'});
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