document.addEventListener('DOMContentLoaded', async function() {
    try {
        const subscriptionsContent = document.getElementById('subscriptions-content');
        if (!subscriptionsContent) {
            console.error('❌ Subscriptions content element not found');
            return;
        }

        console.log('🔍 Loading subscriptions and stats...');

        // Показываем loading state
        subscriptionsContent.innerHTML = `
            <div class="loading">Загрузка подписок...</div>
        `;

        const currentUserId = window.currentUser?.id;
        if (!currentUserId) {
            throw new Error('Пользователь не авторизован');
        }

        // Загружаем данные подписок и статистику параллельно
        const [subscriptionsResponse, statsResponse] = await Promise.all([
            fetch('/api/friends/subscriptions'),
            fetch(`/api/profile/${currentUserId}/subscription-stats`)
        ]);

        if (!subscriptionsResponse.ok) {
            throw new Error(`Ошибка загрузки подписок: ${subscriptionsResponse.status}`);
        }

        const subscriptionsData = await subscriptionsResponse.json();
        const statsData = statsResponse.ok ? await statsResponse.json() : { success: false };

        console.log('📦 Subscriptions data:', subscriptionsData);
        console.log('📊 Stats data:', statsData);

        if (!subscriptionsData.success) {
            throw new Error(subscriptionsData.message || 'Ошибка загрузки подписок');
        }

        const subscriptions = subscriptionsData.subscriptions || [];
        const following = subscriptions.map(sub => sub.following).filter(Boolean);

        // Получаем статистику
        const followersCount = statsData.success ? statsData.stats.followers_count : 0;
        const followingCount = statsData.success ? statsData.stats.following_count : following.length;

        console.log('✅ Final stats:', {
            followersCount,
            followingCount,
            usersCount: following.length
        });

        // Создаем HTML
        subscriptionsContent.innerHTML = createSubscriptionsHTML(following, followersCount, followingCount);
        setupSubscriptionsHandlers();

    } catch (error) {
        console.error('❌ Error loading subscriptions:', error);
        const subscriptionsContent = document.getElementById('subscriptions-content');
        if (subscriptionsContent) {
            subscriptionsContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки подписок: ${error.message}</div>
                <div class="page-actions">
                    <a href="/profile" class="btn btn-outline">← Мой профиль</a>
                </div>
            `;
        }
    }
});

function createSubscriptionsHTML(following, followersCount, followingCount) {
    console.log('🎨 Creating HTML for following:', following);

    return `
        <h2>Мои подписки</h2>

        <!-- Статистика -->
        <div class="stats">
            <div class="stat-item">
                <div class="stat-number">${followersCount}</div>
                <div class="stat-label">Подписчиков</div>
            </div>
            <div class="stat-item">
                <div class="stat-number">${followingCount}</div>
                <div class="stat-label">Подписок</div>
            </div>
        </div>

        <!-- Список подписок -->
        <div>
            <h3>Я подписан на (${following.length})</h3>

            ${following.length > 0 ? `
                <div class="grid grid-3">
                    ${following.map(user => {
        console.log('👤 Rendering user:', user);
        const fullName = `${user.first_name || ''} ${user.last_name || ''}`.trim() || 'Без имени';
        const email = user.email || 'Нет email';
        const age = user.birth_date ? calculateAge(user.birth_date) + ' лет' : '';

        return `
                        <div class="card">
                            <h4>${escapeHtml(fullName)}</h4>
                            <p style="color: var(--text-muted); margin: 10px 0;">${escapeHtml(email)}</p>
                            ${age ? `<p>${age}</p>` : ''}
                            <div style="display: flex; gap: 8px; flex-wrap: wrap; margin-top: 15px;">
                                <a href="/profile/${user.id}" class="btn btn-outline btn-small">Профиль</a>
                                <button class="btn btn-danger btn-small unsubscribe-btn" data-user-id="${user.id}">Отписаться</button>
                            </div>
                        </div>
                        `;
    }).join('')}
                </div>
            ` : `
                <div class="empty-state">
                    <p>Вы пока ни на кого не подписаны.</p>
                    <a href="/profiles" class="btn btn-primary">Найти пользователей</a>
                </div>
            `}
        </div>

        <div class="page-actions">
            <a href="/profiles" class="btn btn-primary">Найти ещё пользователей</a>
            <a href="/profile" class="btn btn-outline">← Мой профиль</a>
        </div>
    `;
}

function setupSubscriptionsHandlers() {
    document.addEventListener('click', async function(e) {
        if (e.target.classList.contains('unsubscribe-btn')) {
            const userId = e.target.getAttribute('data-user-id');
            await unsubscribeFromUser(userId);
        }
    });
}

async function unsubscribeFromUser(userId) {
    if (!confirm('Отписаться от пользователя?')) return;

    try {
        const response = await fetch(`/api/friends/unsubscribe/${userId}`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'}
        });

        const data = await response.json();

        if (data.success) {
            showMessage('✅ Отписка выполнена', 'success');
            setTimeout(() => window.location.reload(), 1000);
        } else {
            showMessage(data.message || '❌ Ошибка', 'error');
        }
    } catch (error) {
        console.error('Error unsubscribing:', error);
        showMessage('❌ Ошибка при отписке', 'error');
    }
}

function calculateAge(birthDateStr) {
    if (!birthDateStr) return 0;
    const birthDate = new Date(birthDateStr);
    const today = new Date();
    let age = today.getFullYear() - birthDate.getFullYear();
    const monthDiff = today.getMonth() - birthDate.getMonth();
    if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
        age--;
    }
    return age;
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