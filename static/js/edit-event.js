document.addEventListener('DOMContentLoaded', async function() {
    try {
        const editEventContent = document.getElementById('edit-event-content');
        if (!editEventContent) {
            console.error('Edit event content element not found');
            return;
        }

        const eventId = editEventContent.dataset.eventId;
        if (!eventId) {
            throw new Error('Event ID not found');
        }

        // Загружаем данные события для редактирования
        const response = await fetch(`/api/event/${eventId}`);

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (!data.success) {
            throw new Error(data.message || 'Ошибка загрузки события');
        }

        const event = data.event;
        const currentUser = window.currentUser;

        // Проверяем, является ли пользователь создателем события
        if (!currentUser || currentUser.id !== event.creator_id) {
            editEventContent.innerHTML = `
                <div class="alert alert-error">Доступ запрещен. Вы не являетесь создателем этого события.</div>
                <div class="page-actions">
                    <a href="/event/${eventId}" class="btn btn-outline">← Назад к событию</a>
                </div>
            `;
            return;
        }

        // Создаем форму редактирования
        editEventContent.innerHTML = createEditFormHTML(event);

        // Настраиваем обработчики формы
        setupEditFormHandlers(eventId);

    } catch (error) {
        console.error('Error loading event for editing:', error);
        const editEventContent = document.getElementById('edit-event-content');
        if (editEventContent) {
            editEventContent.innerHTML = `
                <div class="alert alert-error">Ошибка загрузки события: ${error.message}</div>
                <div class="page-actions">
                    <a href="/events" class="btn btn-outline">← Все события</a>
                </div>
            `;
        }
    }
});

function createEditFormHTML(event) {
    const eventDateTime = new Date(event.date_time);
    const localDateTime = new Date(eventDateTime.getTime() - eventDateTime.getTimezoneOffset() * 60000).toISOString().slice(0, 16);

    return `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 30px;">
            <h2>Редактирование события</h2>
            <a href="${event.is_private ? `/event/private/${event.private_key}` : `/event/${event.id}`}" class="btn btn-outline">← Назад к событию</a>
        </div>

        <form id="edit-event-form" class="event-form">
            <div class="form-group">
                <label for="title" class="form-label">Название события *</label>
                <input type="text" id="title" name="title" class="form-control" value="${event.title}" required maxlength="200">
            </div>

            <div class="form-group">
                <label for="description" class="form-label">Описание *</label>
                <textarea id="description" name="description" class="form-control" rows="5" required maxlength="1000">${event.description}</textarea>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="type" class="form-label">Тип события *</label>
                    <select id="type" name="type" class="form-control" required>
                        <option value="concert" ${event.type === 'concert' ? 'selected' : ''}>Концерт</option>
                        <option value="lecture" ${event.type === 'lecture' ? 'selected' : ''}>Лекция</option>
                        <option value="sport" ${event.type === 'sport' ? 'selected' : ''}>Спорт</option>
                        <option value="meeting" ${event.type === 'meeting' ? 'selected' : ''}>Встреча</option>
                        <option value="party" ${event.type === 'party' ? 'selected' : ''}>Вечеринка</option>
                        <option value="conference" ${event.type === 'conference' ? 'selected' : ''}>Конференция</option>
                        <option value="exhibition" ${event.type === 'exhibition' ? 'selected' : ''}>Выставка</option>
                        <option value="other" ${event.type === 'other' ? 'selected' : ''}>Другое</option>
                    </select>
                </div>
                <div class="form-group">
                    <label for="max_participants" class="form-label">Макс. участников</label>
                    <input type="number" id="max_participants" name="max_participants" value="${event.max_participants || ''}" class="form-control" min="1" placeholder="Неограниченно">
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="date_time" class="form-label">Дата и время *</label>
                    <input type="datetime-local" id="date_time" name="date_time" value="${localDateTime}" class="form-control" required>
                </div>
                <div class="form-group">
                    <label for="location" class="form-label">Место проведения *</label>
                    <input type="text" id="location" name="location" value="${event.location}" class="form-control" required>
                </div>
            </div>

            <div class="form-row">
                <div class="form-group">
                    <label for="latitude" class="form-label">Широта</label>
                    <input type="number" id="latitude" name="latitude" value="${event.latitude || ''}" class="form-control" step="any" placeholder="55.7558">
                </div>
                <div class="form-group">
                    <label for="longitude" class="form-label">Долгота</label>
                    <input type="number" id="longitude" name="longitude" value="${event.longitude || ''}" class="form-control" step="any" placeholder="37.6173">
                </div>
            </div>

            <div class="form-group">
                <label class="checkbox-label">
                    <input type="checkbox" id="is_private" name="is_private" ${event.is_private ? 'checked' : ''}>
                    <span class="checkmark"></span>
                    Приватное событие
                </label>
            </div>

            <div style="display: flex; gap: 10px; justify-content: flex-end; margin-top: 30px; padding-top: 20px; border-top: 1px solid var(--border-color);">
                <a href="${event.is_private ? `/event/private/${event.private_key}` : `/event/${event.id}`}" class="btn btn-outline">Отмена</a>
                <button type="submit" class="btn btn-primary">Сохранить изменения</button>
            </div>
        </form>
    `;
}

function setupEditFormHandlers(eventId) {
    const form = document.getElementById('edit-event-form');
    const dateTimeInput = document.getElementById('date_time');

    if (!form) return;

    // Валидация даты
    if (dateTimeInput) {
        const now = new Date();
        dateTimeInput.addEventListener('change', function() {
            const selectedDate = new Date(this.value);

            if (selectedDate < now) {
                showMessage('❌ Нельзя устанавливать дату в прошлом', 'error');
                const eventDateTime = new Date();
                const localDateTime = new Date(eventDateTime.getTime() - eventDateTime.getTimezoneOffset() * 60000).toISOString().slice(0, 16);
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
            const response = await fetch(`/api/events/${eventId}/update`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify(eventData)
            });

            const data = await response.json();

            if (data.success) {
                showMessage('✅ Событие успешно обновлено!', 'success');
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
            console.error('Error updating event:', error);
            showMessage('❌ Ошибка при обновлении события', 'error');
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

    const form = document.getElementById('edit-event-form');
    if (form) {
        form.parentNode.insertBefore(alert, form);
    }

    if (type === 'success') {
        setTimeout(() => {
            alert.remove();
        }, 3000);
    }
}
