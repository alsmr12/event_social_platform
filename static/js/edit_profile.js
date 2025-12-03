document.addEventListener('DOMContentLoaded', function() {
    initializeEditProfile();
});

async function initializeEditProfile() {
    try {
        const container = document.getElementById('edit-profile-content');
        if (!container) return;

        // Показываем загрузку
        container.innerHTML = `
            <div class="loading">Загрузка данных профиля...</div>
        `;

        // Загружаем данные профиля
        const profileData = await loadProfileData();

        // Рендерим форму
        renderEditForm(container, profileData);

        // Настраиваем обработчики
        setupEventHandlers();

    } catch (error) {
        console.error('Error initializing edit profile:', error);
        const container = document.getElementById('edit-profile-content');
        container.innerHTML = `
            <div class="alert alert-error">
                Ошибка загрузки формы: ${error.message}
                <br><br>
                <a href="/profile" class="btn btn-outline">Вернуться в профиль</a>
            </div>
        `;
    }
}

async function loadProfileData() {
    try {
        const response = await fetch('/api/profile');
        if (!response.ok) throw new Error(`HTTP error! status: ${response.status}`);

        const data = await response.json();
        if (!data.user) throw new Error('Данные пользователя не найдены');

        return {
            user: data.user,
            socialLinks: data.social_links || []
        };
    } catch (error) {
        console.error('Error loading profile data:', error);
        throw error;
    }
}

function renderEditForm(container, data) {
    const { user, socialLinks } = data;

    container.innerHTML = `
        <h2 style="text-align: center; margin-bottom: 30px;">Редактирование профиля</h2>

        <div id="error-message" class="alert alert-error" style="display: none;"></div>
        <div id="success-message" class="alert alert-success" style="display: none;"></div>

        <form id="edit-profile-form">
            <div class="form-group">
                <label for="email">Email</label>
                <input type="email" id="email" class="form-control" value="${user.email || ''}" readonly style="background: var(--light-bg);">
                <small style="color: var(--text-muted);">Email нельзя изменить</small>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="first_name">Имя *</label>
                    <input type="text" id="first_name" name="first_name" class="form-control" value="${user.first_name || ''}" required>
                </div>
                <div class="form-group">
                    <label for="last_name">Фамилия *</label>
                    <input type="text" id="last_name" name="last_name" class="form-control" value="${user.last_name || ''}" required>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="gender">Пол</label>
                    <select id="gender" name="gender" class="form-control">
                        <option value="">Не указано</option>
                        <option value="male" ${user.gender === 'male' ? 'selected' : ''}>Мужской</option>
                        <option value="female" ${user.gender === 'female' ? 'selected' : ''}>Женский</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="birth_date">Дата рождения</label>
                    <input type="date" id="birth_date" name="birth_date" class="form-control" value="${formatDateForInput(user.birth_date)}">
                    <small style="color: var(--text-muted);">Возраст: <span id="age-display">${user.age || 0}</span> <span id="age-unit">${user.age_text || 'лет'}</span></small>
                </div>
            </div>

            <div class="form-group">
                <label for="phone">Телефон</label>
                <input type="tel" id="phone" name="phone" class="form-control" value="${user.phone || ''}" placeholder="+7 (XXX) XXX-XX-XX">
            </div>

            <!-- Социальные сети -->
            <div class="form-group">
                <label>Социальные сети</label>
                <div id="social-links-container">
                    ${renderSocialLinks(socialLinks)}
                </div>

                <div style="margin: 10px 0;">
                    <button type="button" class="btn btn-outline" onclick="addSocialLink()">+ Добавить соцсеть</button>
                </div>
            </div>

            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 30px; padding-top: 20px; border-top: 1px solid var(--border-color);">
                <a href="/profile" class="btn btn-secondary">Отмена</a>
                <button type="submit" class="btn btn-primary">Сохранить изменения</button>
            </div>
        </form>
    `;
}

function renderSocialLinks(socialLinks) {
    if (socialLinks.length === 0) {
        return createSocialLinkHTML();
    }

    return socialLinks.map(link => createSocialLinkHTML(link)).join('');
}

function createSocialLinkHTML(link = {}) {
    return `
        <div class="social-link-item" style="margin-bottom: 15px; padding: 15px; border: 1px solid var(--border-color); border-radius: 8px;">
            <div class="grid grid-3" style="gap: 10px;">
                <div>
                    <label>Платформа</label>
                    <select name="platform" class="form-control social-platform" required>
                        <option value="">Выберите платформу</option>
                        <option value="vk" ${link.platform === 'vk' ? 'selected' : ''}>VK</option>
                        <option value="tg" ${link.platform === 'tg' ? 'selected' : ''}>Telegram</option>
                        <option value="custom" ${link.platform === 'custom' ? 'selected' : ''}>Другая сеть</option>
                    </select>
                </div>
                <div>
                    <label>Username/ID</label>
                    <input type="text" name="username" class="form-control social-username" value="${link.username || ''}" placeholder="username или id" required>
                </div>
                <div>
                    <label>Название (для кастомных)</label>
                    <input type="text" name="custom_name" class="form-control social-custom-name" value="${link.custom_name || ''}" placeholder="Название соцсети">
                </div>
            </div>
            <button type="button" class="btn btn-danger btn-small" onclick="removeSocialLink(this)" style="margin-top: 10px;">Удалить</button>
        </div>
    `;
}

function setupEventHandlers() {
    const form = document.getElementById('edit-profile-form');
    const birthDateInput = document.getElementById('birth_date');

    // Обновление возраста при изменении даты рождения
    if (birthDateInput) {
        birthDateInput.addEventListener('change', updateAgeDisplay);

        // Устанавливаем максимальную дату (сегодня)
        const today = new Date().toISOString().split('T')[0];
        birthDateInput.setAttribute('max', today);
    }

    // Обработка отправки формы
    if (form) {
        form.addEventListener('submit', handleFormSubmit);
    }
}

function updateAgeDisplay() {
    const birthDateInput = document.getElementById('birth_date');
    const ageDisplay = document.getElementById('age-display');
    const ageUnit = document.getElementById('age-unit');

    if (birthDateInput && ageDisplay && ageUnit && birthDateInput.value) {
        const birthDate = new Date(birthDateInput.value);
        const today = new Date();
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();

        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }

        ageDisplay.textContent = age;

        // Определяем правильное окончание для слова "год"
        let ageText;
        if (age === 1 || (age % 10 === 1 && age % 100 !== 11)) {
            ageText = 'год';
        } else if (age >= 2 && age <= 4 || 
                 (age % 10 >= 2 && age % 10 <= 4 && !(age % 100 >= 12 && age % 100 <= 14))) {
            ageText = 'года';
        } else {
            ageText = 'лет';
        }
        
        ageUnit.textContent = ageText;
    }
}

async function handleFormSubmit(event) {
    event.preventDefault();

    try {
        showMessage('success', '', true); // Скрываем успех
        showMessage('error', '', true); // Скрываем ошибку

        const profileData = collectFormData();

        // Валидация
        if (!profileData.first_name || !profileData.last_name) {
            showMessage('error', 'Имя и фамилия обязательны для заполнения');
            return;
        }

        // Сохраняем основной профиль
        const profileResponse = await fetch('/api/profile', {
            method: 'PUT',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify(profileData)
        });

        const profileResult = await profileResponse.json();

        if (!profileResult.success) {
            throw new Error(profileResult.message || 'Ошибка сохранения профиля');
        }

        // Сохраняем социальные сети
        await saveSocialLinks(profileData.social_links);

        showMessage('success', 'Профиль успешно обновлен!');

        // Перезагружаем страницу через 2 секунды
        setTimeout(() => {
            window.location.href = '/profile';
        }, 2000);

    } catch (error) {
        console.error('Error saving profile:', error);
        showMessage('error', 'Ошибка при сохранении профиля: ' + error.message);
    }
}

function collectFormData() {
    const form = document.getElementById('edit-profile-form');
    const formData = new FormData(form);

    // Собираем основные данные
    const data = {
        first_name: document.getElementById('first_name').value,
        last_name: document.getElementById('last_name').value,
        gender: document.getElementById('gender').value,
        birth_date: document.getElementById('birth_date').value,
        phone: document.getElementById('phone').value
    };

    // Собираем социальные сети
    const socialLinks = [];
    const socialItems = document.querySelectorAll('.social-link-item');

    socialItems.forEach(item => {
        const platform = item.querySelector('.social-platform').value;
        const username = item.querySelector('.social-username').value;
        const customName = item.querySelector('.social-custom-name').value;

        if (platform && username) {
            socialLinks.push({
                platform: platform,
                username: username,
                custom_name: customName || ''
            });
        }
    });

    data.social_links = socialLinks;

    return data;
}

async function saveSocialLinks(socialLinks) {
    try {
        const response = await fetch('/social-links', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
            },
            body: JSON.stringify({ social_links: socialLinks })
        });

        if (!response.ok) {
            const result = await response.json();
            throw new Error(result.message || 'Ошибка сохранения социальных сетей');
        }
    } catch (error) {
        console.error('Error saving social links:', error);
        // Не прерываем основной поток, просто логируем ошибку
    }
}

// Глобальные функции для работы с социальными сетями
window.addSocialLink = function() {
    const container = document.getElementById('social-links-container');
    if (!container) return;

    const newItem = document.createElement('div');
    newItem.innerHTML = createSocialLinkHTML();
    container.appendChild(newItem);
}

window.removeSocialLink = function(button) {
    const item = button.closest('.social-link-item');
    if (item) {
        item.remove();
    }
}

// Вспомогательные функции
function formatDateForInput(dateString) {
    if (!dateString) return '';

    try {
        const date = new Date(dateString);
        return date.toISOString().split('T')[0];
    } catch (error) {
        return '';
    }
}

function showMessage(type, message, hide = false) {
    const element = document.getElementById(`${type}-message`);
    if (!element) return;

    if (hide) {
        element.style.display = 'none';
    } else {
        element.textContent = message;
        element.style.display = 'block';

        // Автоскрытие через 5 секунд для ошибок
        if (type === 'error') {
            setTimeout(() => {
                element.style.display = 'none';
            }, 5000);
        }
    }
}