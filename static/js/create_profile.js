
document.addEventListener('DOMContentLoaded', function() {
    const createProfileForm = document.getElementById('create-profile-form');
    const phoneInput = document.getElementById('phone');
    const birthDateInput = document.getElementById('birth_date');
    const ageDisplay = document.getElementById('age-display');

    // Инициализация маски телефона и расчета возраста
    if (phoneInput) {
        initPhoneMask(phoneInput);
    }

    if (birthDateInput && ageDisplay) {
        initAgeCalculator(birthDateInput, ageDisplay);
    }

    if (createProfileForm) {
        createProfileForm.addEventListener('submit', async function(e) {
            e.preventDefault();

            const formData = new FormData(createProfileForm);
            const profileData = {
                email: formData.get('email'),
                password: formData.get('password'),
                first_name: formData.get('first_name'),
                last_name: formData.get('last_name'),
                gender: formData.get('gender'),
                birth_date: formData.get('birth_date'),
                phone: formData.get('phone')
            };

            // Валидация
            if (!profileData.email || !profileData.password || !profileData.first_name || !profileData.last_name) {
                showMessage('❌ Заполните все обязательные поля', 'error');
                return;
            }

            if (profileData.password.length < 6) {
                showMessage('❌ Пароль должен содержать минимум 6 символов', 'error');
                return;
            }

            // Валидация email
            if (!isValidEmail(profileData.email)) {
                showMessage('❌ Введите корректный email', 'error');
                return;
            }

            // Валидация телефона (если заполнен)
            if (profileData.phone && !isValidPhone(profileData.phone)) {
                showMessage('❌ Введите корректный номер телефона', 'error');
                return;
            }

            // Валидация даты рождения (если заполнена)
            if (profileData.birth_date) {
                const birthDate = new Date(profileData.birth_date);
                const today = new Date();

                if (birthDate > today) {
                    showMessage('❌ Дата рождения не может быть в будущем', 'error');
                    return;
                }
            }

            try {
                const response = await fetch('/api/register', {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                    },
                    body: JSON.stringify(profileData)
                });

                const data = await response.json();

                if (data.success) {
                    showMessage('✅ Профиль создан успешно! Выполняется вход...', 'success');
                    setTimeout(() => {
                        window.location.href = data.redirect_url || '/';
                    }, 1500);
                } else {
                    showMessage(`❌ Ошибка: ${data.message}`, 'error');
                }
            } catch (error) {
                console.error('Create profile error:', error);
                showMessage('❌ Ошибка при создании профиля', 'error');
            }
        });
    }
});

// Функции для маски телефона
function initPhoneMask(phoneInput) {
    phoneInput.addEventListener('input', function(e) {
        let value = e.target.value.replace(/\D/g, '');

        if (value.startsWith('7') || value.startsWith('8')) {
            value = '7' + value.substring(1);
        } else if (value.startsWith('9') && value.length === 10) {
            value = '7' + value;
        }

        if (value.length > 0) {
            value = '+7' + value.substring(1);
        }

        if (value.length > 2) {
            value = value.substring(0, 2) + ' (' + value.substring(2);
        }
        if (value.length > 7) {
            value = value.substring(0, 7) + ') ' + value.substring(7);
        }
        if (value.length > 12) {
            value = value.substring(0, 12) + '-' + value.substring(12);
        }
        if (value.length > 15) {
            value = value.substring(0, 15) + '-' + value.substring(15);
        }

        e.target.value = value.substring(0, 18);
    });

    // Подсказка при фокусе
    phoneInput.addEventListener('focus', function() {
        if (!this.value) {
            this.value = '+7 (';
        }
    });

    // Очистка при backspace
    phoneInput.addEventListener('keydown', function(e) {
        if (e.key === 'Backspace' && this.value === '+7 (') {
            this.value = '';
        }
    });
}

// Функции для расчета возраста
function initAgeCalculator(birthDateInput, ageDisplay) {
    function calculateAge(birthDate) {
        const today = new Date();
        let age = today.getFullYear() - birthDate.getFullYear();
        const monthDiff = today.getMonth() - birthDate.getMonth();
        if (monthDiff < 0 || (monthDiff === 0 && today.getDate() < birthDate.getDate())) {
            age--;
        }
        return age;
    }

    function updateAgeDisplay() {
        const birthDateValue = birthDateInput.value;
        if (birthDateValue) {
            const birthDate = new Date(birthDateValue);
            const age = calculateAge(birthDate);
            ageDisplay.textContent = age;
            
            // Определяем правильное окончание для слова "год"
            let ageUnit;
            if (age === 1 || (age % 10 === 1 && age % 100 !== 11)) {
                ageUnit = 'год';
            } else if (age >= 2 && age <= 4 || 
                     (age % 10 >= 2 && age % 10 <= 4 && !(age % 100 >= 12 && age % 100 <= 14))) {
                ageUnit = 'года';
            } else {
                ageUnit = 'лет';
            }
            
            document.getElementById('age-unit').textContent = ageUnit;
        } else {
            ageDisplay.textContent = '0';
            document.getElementById('age-unit').textContent = 'лет';
        }
    }

    birthDateInput.addEventListener('change', updateAgeDisplay);

    // Установка максимальной даты (сегодня)
    const today = new Date().toISOString().split('T')[0];
    birthDateInput.setAttribute('max', today);
    birthDateInput.setAttribute('min', '1900-01-01');

    // Инициализация
    updateAgeDisplay();
}

// Валидация email
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

// Валидация телефона
function isValidPhone(phone) {
    const phoneRegex = /^\+7\s?\(\d{3}\)\s?\d{3}-\d{2}-\d{2}$/;
    return phoneRegex.test(phone);
}

function showMessage(message, type) {
    // Удаляем существующие сообщения
    const existingAlerts = document.querySelectorAll('.form-alert');
    existingAlerts.forEach(alert => alert.remove());

    const alert = document.createElement('div');
    alert.className = `alert alert-${type} form-alert`;
    alert.style.marginBottom = '20px';
    alert.textContent = message;

    const form = document.getElementById('create-profile-form');
    if (form) {
        form.parentNode.insertBefore(alert, form);
    }

    if (type === 'success') {
        setTimeout(() => {
            alert.remove();
        }, 3000);
    }
}
