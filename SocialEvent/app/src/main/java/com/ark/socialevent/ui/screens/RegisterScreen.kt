package com.ark.socialevent.ui.screens

import android.app.DatePickerDialog
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.UserRepository
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    userRepo: UserRepository,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthday by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }

    // Ошибки по полям
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var emailError by remember { mutableStateOf("") }
    var passwordError by remember { mutableStateOf("") }
    var genderError by remember { mutableStateOf("") }
    var birthdayError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    val genders = listOf("Мужской", "Женский")
    var genderExpanded by remember { mutableStateOf(false) }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            birthday = "%02d/%02d/%04d".format(dayOfMonth, month + 1, year)
            birthdayError = ""
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "Создание профиля",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )

        Text(
            "Заполните информацию о себе",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Имя и Фамилия в одной строке
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Имя
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it; firstNameError = "" },
                label = { Text("Имя") },
                singleLine = true,
                isError = firstNameError.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )

            // Фамилия
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it; lastNameError = "" },
                label = { Text("Фамилия") },
                singleLine = true,
                isError = lastNameError.isNotEmpty(),
                modifier = Modifier.weight(1f)
            )
        }

        if (firstNameError.isNotEmpty() || lastNameError.isNotEmpty()) {
            Row {
                if (firstNameError.isNotEmpty()) {
                    Text(
                        firstNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (lastNameError.isNotEmpty()) {
                    Text(
                        lastNameError,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Email
        OutlinedTextField(
            value = email,
            onValueChange = { email = it; emailError = "" },
            label = { Text("Email") },
            singleLine = true,
            isError = emailError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        if (emailError.isNotEmpty()) {
            Text(
                emailError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Пароль
        OutlinedTextField(
            value = password,
            onValueChange = { password = it; passwordError = "" },
            label = { Text("Пароль") },
            singleLine = true,
            isError = passwordError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        if (passwordError.isNotEmpty()) {
            Text(
                passwordError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Пол
        ExposedDropdownMenuBox(
            expanded = genderExpanded,
            onExpandedChange = { genderExpanded = !genderExpanded },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = gender,
                onValueChange = {},
                readOnly = true,
                label = { Text("Пол") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = genderExpanded
                    )
                },
                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                isError = genderError.isNotEmpty(),
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            ExposedDropdownMenu(
                expanded = genderExpanded,
                onDismissRequest = { genderExpanded = false }
            ) {
                genders.forEach { g ->
                    DropdownMenuItem(
                        text = { Text(g) },
                        onClick = {
                            gender = g
                            genderExpanded = false
                            genderError = ""
                        }
                    )
                }
            }
        }
        if (genderError.isNotEmpty()) {
            Text(
                genderError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Дата рождения
        OutlinedButton(
            onClick = { datePicker.show() },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.outlinedButtonColors()
        ) {
            Text(
                if (birthday.isEmpty()) "Выберите дату рождения"
                else "Дата рождения: $birthday"
            )
        }
        if (birthdayError.isNotEmpty()) {
            Text(
                birthdayError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Телефон
        OutlinedTextField(
            value = phone,
            onValueChange = {
                // Базовая маска для телефона
                val cleaned = it.filter { char -> char.isDigit() }
                if (cleaned.length <= 11) {
                    phone = when {
                        cleaned.isEmpty() -> ""
                        cleaned.length == 1 -> "+7 ($cleaned"
                        cleaned.length <= 4 -> "+7 (${cleaned.substring(1)}"
                        cleaned.length <= 7 -> "+7 (${cleaned.substring(1, 4)}) ${cleaned.substring(4)}"
                        cleaned.length <= 9 -> "+7 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
                        else -> "+7 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7, 9)}-${cleaned.substring(9)}"
                    }
                    phoneError = ""
                }
            },
            label = { Text("Телефон") },
            placeholder = { Text("+7 (xxx) xxx-xx-xx") },
            singleLine = true,
            isError = phoneError.isNotEmpty(),
            modifier = Modifier.fillMaxWidth()
        )
        if (phoneError.isNotEmpty()) {
            Text(
                phoneError,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка создания профиля
        Button(
            onClick = {
                // Проверки
                var hasError = false

                if (firstName.isBlank()) {
                    firstNameError = "Введите имя"
                    hasError = true
                }
                if (lastName.isBlank()) {
                    lastNameError = "Введите фамилию"
                    hasError = true
                }
                if (!email.contains("@")) {
                    emailError = "Неверный формат email"
                    hasError = true
                }
                if (password.length < 6) {
                    passwordError = "Пароль должен содержать минимум 6 символов"
                    hasError = true
                }
                if (gender != "Мужской" && gender != "Женский") {
                    genderError = "Выберите пол"
                    hasError = true
                }
                if (birthday.isEmpty()) {
                    birthdayError = "Выберите дату рождения"
                    hasError = true
                }
                if (!Regex("""\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}""").matches(phone)) {
                    phoneError = "Введите корректный номер телефона"
                    hasError = true
                }

                if (!hasError) {
                    userRepo.register(
                        email, password, firstName, lastName, gender,
                        0, // сервер обрабатывает дату рождения
                        phone
                    ) { success, msg ->
                        if (success) {
                            Toast.makeText(context, "Профиль успешно создан", Toast.LENGTH_SHORT).show()
                            onRegisterSuccess()
                        } else {
                            Toast.makeText(context, "Ошибка: $msg", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Создать профиль", style = MaterialTheme.typography.labelLarge)
        }

        // Ссылка на вход
        TextButton(
            onClick = onNavigateToLogin,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Уже есть аккаунт? Войти")
        }
    }
}