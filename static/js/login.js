
document.addEventListener('DOMContentLoaded', function() {
    const loginForm = document.getElementById('login-form');

    if (loginForm) {
        loginForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(loginForm);
            const loginData = {
                email: formData.get('email'),
                password: formData.get('password')
            };

            // Валидация
            if (!loginData.email || !loginData.password) {
                showMessage('❌ Заполните все поля', 'error');
                return;
            }

            try {
                const response = await fetch('/api/login', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(loginData)
                });

                const data = await response.json();

                if (data.success) {
                    showMessage('✅ Вход выполнен успешно!', 'success');
                    setTimeout(() => {
                        window.location.href = data.redirect_url || '/';
                    }, 1000);
                } else {
                    showMessage(`❌ Ошибка: ${data.message}`, 'error');
                }
            } catch (error) {
                console.error('Login error:', error);
                showMessage('❌ Ошибка при входе в систему', 'error');
            }
        });
    }
});

function showMessage(message, type) {
    // Удаляем существующие сообщения
    const existingAlerts = document.querySelectorAll('.form-alert');
    existingAlerts.forEach(alert => alert.remove());

    const alert = document.createElement('div');
    alert.className = `alert alert-${type} form-alert`;
    alert.style.marginBottom = '20px';
    alert.textContent = message;

    const form = document.getElementById('login-form');
    if (form) {
        form.parentNode.insertBefore(alert, form);
    }

    if (type === 'success') {
        setTimeout(() => {
            alert.remove();
        }, 3000);
    }
}