document.addEventListener('DOMContentLoaded', function() {
    try {
        const createEventContent = document.getElementById('create-event-content');
        if (!createEventContent) {
            console.error('Create event content element not found');
            return;
        }

        // Создаем форму создания события
        createEventContent.innerHTML = createEventFormHTML();

        // Настраиваем обработчики формы
        setupCreateFormHandlers();

    } catch (error) {
        console.error('Error loading create event form:', error);
        const createEventContent = document.getElementById('create-event-content');
        if (createEventContent) {
            createEventContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки формы: ${error.message}</div>
                <div class="page-actions">
                    <a href="/events" class="btn btn-outline">← Все события</a>
                </div>
            `;
        }
    }
});

function createEventFormHTML() {
    const now = new Date();
    const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

    return `
        <h2 style="text-align: center; margin-bottom: 30px;">Создание события</h2>

        <form id="create-event-form" class="event-form">
            <div class="form-group">
                <label for="title" class="form-label">Название события *</label>
                <input type="text" id="title" name="title" class="form-control" required maxlength="200" placeholder="Введите название события">
            </div>

            <div class="form-group">
                <label for="description" class="form-label">Описание *</label>
                <textarea id="description" name="description" class="form-control" rows="5" required maxlength="1000" placeholder="Опишите ваше событие"></textarea>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="type" class="form-label">Тип события *</label>
                    <select id="type" name="type" class="form-control" required>
                        <option value="">Выберите тип</option>
                        <option value="concert">Концерт</option>
                        <option value="lecture">Лекция</option>
                        <option value="sport">Спорт</option>
                        <option value="meeting">Встреча</option>
                        <option value="party">Вечеринка</option>
                        <option value="conference">Конференция</option>
                        <option value="exhibition">Выставка</option>
                        <option value="other">Другое</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="date_time" class="form-label">Дата и время *</label>
                    <input type="datetime-local" id="date_time" name="date_time" class="form-control" required min="${localDateTime}">
                </div>
            </div>

            <div class="form-group">
                <label for="location" class="form-label">Место проведения *</label>
                <input type="text" id="location" name="location" class="form-control" required placeholder="Укажите место проведения">
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="latitude" class="form-label">Широта</label>
                    <input type="number" id="latitude" name="latitude" class="form-control" step="any" placeholder="55.7558">
                </div>
                <div class="form-group">
                    <label for="longitude" class="form-label">Долгота</label>
                    <input type="number" id="longitude" name="longitude" class="form-control" step="any" placeholder="37.6173">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="max_participants" class="form-label">Макс. участников</label>
                    <input type="number" id="max_participants" name="max_participants" class="form-control" min="1" placeholder="Неограниченно">
                </div>
                <div class="form-group">
                    <label class="checkbox-label">
                        <input type="checkbox" id="is_private" name="is_private">
                        <span class="checkmark"></span>
                        Приватное событие
                    </label>
                    <small style="color: var(--text-muted); display: block; margin-top: 5px;">Только по приглашению</small>
                </div>
            </div>

            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 30px;">
                <a href="/events" class="btn btn-outline">Отмена</a>
                <button type="submit" class="btn btn-primary">Создать событие</button>
            </div>
        </form>
    `;
}

function setupCreateFormHandlers() {
    const form = document.getElementById('create-event-form');
    const dateTimeInput = document.getElementById('date_time');

    if (!form) return;

    // Валидация даты
    if (dateTimeInput) {
        dateTimeInput.addEventListener('change', function() {
            const selectedDate = new Date(this.value);
            const now = new Date();

            if (selectedDate < now) {
                showMessage('❌ Нельзя создавать события в прошлом', 'error');
                const localDateTime = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
                this.value = localDateTime;
            }
        });
    }

    // Обработчик отправки формы
    form.addEventListener('submit', async function(e) {
        e.preventDefault();

        // Собираем данные формы
        const formData = new FormData(form);
        const eventData = {
            title: formData.get('title'),
            description: formData.get('description'),
            type: formData.get('type'),
            date_time: formData.get('date_time'),
            location: formData.get('location'),
            is_private: formData.get('is_private') === 'on'
        };

        // Добавляем опциональные поля
        const maxParticipants = formData.get('max_participants');
        if (maxParticipants) {
            eventData.max_participants = parseInt(maxParticipants);
        }

        const latitude = formData.get('latitude');
        const longitude = formData.get('longitude');
        if (latitude) eventData.latitude = latitude;
        if (longitude) eventData.longitude = longitude;

        // Валидация
        if (!eventData.title || !eventData.description || !eventData.type || !eventData.date_time || !eventData.location) {
            showMessage('❌ Заполните все обязательные поля', 'error');
            return;
        }

        try {
            const response = await fetch('/api/create-event', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(eventData)
            });

            const data = await response.json();

            if (data.success) {
                showMessage('✅ Событие успешно создано!', 'success');
                setTimeout(() => {
                    if (data.event.is_private) {
                        window.location.href = `/event/private/${data.event.private_key}`;
                    } else {
                        window.location.href = `/event/${data.event.id}`;
                    }
                }, 1500);
            } else {
                showMessage(`❌ Ошибка: ${data.message}`, 'error');
            }
        } catch (error) {
            console.error('Error creating event:', error);
            showMessage('❌ Ошибка при создании события', 'error');
        }
    });

    // Валидация максимального количества участников
    const maxParticipantsInput = document.getElementById('max_participants');
    if (maxParticipantsInput) {
        maxParticipantsInput.addEventListener('input', function() {
            if (this.value && this.value < 1) {
                this.value = 1;
            }
        });
    }
}

function showMessage(message, type) {
    // Удаляем существующие сообщения
    const existingAlerts = document.querySelectorAll('.form-alert');
    existingAlerts.forEach(alert => alert.remove());

    const alert = document.createElement('div');
    alert.className = `alert alert-${type} form-alert`;
    alert.style.marginBottom = '20px';
    alert.textContent = message;

    const form = document.getElementById('create-event-form');
    if (form) {
        form.parentNode.insertBefore(alert, form);
    }

    if (type === 'success') {
        setTimeout(() => {
            alert.remove();
        }, 3000);
    }
}