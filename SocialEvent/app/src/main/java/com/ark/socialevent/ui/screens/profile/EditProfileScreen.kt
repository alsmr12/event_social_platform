package com.ark.socialevent.ui.screens.profile

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.UserProfile
import com.ark.socialevent.network.UserRepository
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userRepository: UserRepository,
    currentProfile: UserProfile?,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val context = LocalContext.current

    var firstName by remember { mutableStateOf(currentProfile?.firstName ?: "") }
    var lastName by remember { mutableStateOf(currentProfile?.lastName ?: "") }
    var gender by remember { mutableStateOf(currentProfile?.gender ?: "") }
    var birthDate by remember { mutableStateOf(currentProfile?.birthDate ?: "") }
    var phone by remember { mutableStateOf(currentProfile?.phone ?: "") }

    // Ошибки по полям
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var genderError by remember { mutableStateOf("") }
    var birthDateError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Выпадающий список для пола
    var genderExpanded by remember { mutableStateOf(false) }
    val genders = listOf("Мужской", "Женский")

    // Функция для преобразования даты в формат "2006-01-02"
    fun formatBirthDateForAPI(day: Int, month: Int, year: Int): String {
        return String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day)
    }

    // Функция для отображения даты в формате "dd/MM/yyyy"
    fun formatBirthDateForDisplay(day: Int, month: Int, year: Int): String {
        return "%02d/%02d/%04d".format(day, month + 1, year)
    }

    // Функция для вычисления возраста из даты рождения
    fun calculateAge(year: Int, month: Int, day: Int): Int {
        val birthDate = Calendar.getInstance().apply {
            set(year, month, day)
        }
        val today = Calendar.getInstance()

        var age = today.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)

        // Проверяем, был ли уже день рождения в этом году
        if (today.get(Calendar.DAY_OF_YEAR) < birthDate.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        return age
    }

    // Переменные для хранения выбранной даты
    var selectedYear by remember { mutableStateOf(0) }
    var selectedMonth by remember { mutableStateOf(0) }
    var selectedDay by remember { mutableStateOf(0) }

    // Инициализация даты из текущего профиля
    LaunchedEffect(currentProfile?.birthDate) {
        currentProfile?.birthDate?.let { date ->
            try {
                val parts = date.split("-")
                if (parts.size == 3) {
                    selectedYear = parts[0].toInt()
                    selectedMonth = parts[1].toInt() - 1
                    selectedDay = parts[2].toInt()
                    birthDate = formatBirthDateForDisplay(selectedDay, selectedMonth, selectedYear)
                }
            } catch (e: Exception) {
                // Если не удалось распарсить, оставляем пустым
            }
        }
    }

    val calendar = Calendar.getInstance()
    val datePicker = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            selectedYear = year
            selectedMonth = month
            selectedDay = dayOfMonth
            birthDate = formatBirthDateForDisplay(dayOfMonth, month, year)
            birthDateError = ""
        },
        // Устанавливаем начальную дату из профиля или 25 лет назад
        selectedYear.takeIf { it != 0 } ?: (calendar.get(Calendar.YEAR) - 25),
        selectedMonth.takeIf { it != 0 } ?: calendar.get(Calendar.MONTH),
        selectedDay.takeIf { it != 0 } ?: calendar.get(Calendar.DAY_OF_MONTH)
    ).apply {
        // Устанавливаем максимальную дату - сегодня
        datePicker?.maxDate = calendar.timeInMillis
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Редактирование профиля") },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = {
                                // Сброс ошибок
                                firstNameError = ""
                                lastNameError = ""
                                genderError = ""
                                birthDateError = ""
                                phoneError = ""
                                errorMessage = null

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
                                if (gender != "Мужской" && gender != "Женский") {
                                    genderError = "Выберите пол"
                                    hasError = true
                                }
                                if (birthDate.isEmpty()) {
                                    birthDateError = "Выберите дату рождения"
                                    hasError = true
                                }

                                // Проверка формата телефона
                                val phoneRegex = Regex("""\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}""")
                                if (!phoneRegex.matches(phone)) {
                                    phoneError = "Введите корректный номер телефона в формате +7 (xxx) xxx-xx-xx"
                                    hasError = true
                                }

                                if (!hasError) {
                                    // Проверяем что дата выбрана
                                    if (selectedYear == 0) {
                                        birthDateError = "Выберите дату рождения"
                                        return@IconButton
                                    }

                                    // ВЫЧИСЛЯЕМ ВОЗРАСТ для проверки
                                    val age = calculateAge(selectedYear, selectedMonth, selectedDay)

                                    // Проверяем что возраст корректен
                                    if (age < 14) {
                                        birthDateError = "Возраст должен быть не менее 14 лет"
                                        return@IconButton
                                    }
                                    if (age > 100) {
                                        birthDateError = "Возраст должен быть не более 100 лет"
                                        return@IconButton
                                    }

                                    isLoading = true
                                    successMessage = null

                                    // ПОДГОТАВЛИВАЕМ ДАТУ ДЛЯ API в формате "2006-01-02"
                                    val birthDateForAPI = formatBirthDateForAPI(selectedDay, selectedMonth, selectedYear)

                                    // РЕАЛЬНЫЙ ВЫЗОВ API С ДАТОЙ РОЖДЕНИЯ
                                    userRepository.updateProfile(
                                        firstName = firstName,
                                        lastName = lastName,
                                        gender = gender,
                                        birthDate = birthDateForAPI, // ПЕРЕДАЕМ ДАТУ РОЖДЕНИЯ
                                        phone = phone
                                    ) { success, message, updatedUser ->
                                        isLoading = false
                                        if (success) {
                                            successMessage = message ?: "Профиль успешно обновлен!"
                                            // Автоматически возвращаемся через 2 секунды
                                            coroutineScope.launch {
                                                delay(2000)
                                                onSaveSuccess()
                                            }
                                        } else {
                                            errorMessage = message ?: "Ошибка обновления профиля"
                                        }
                                    }
                                }
                            },
                            enabled = !isLoading
                        ) {
                            Icon(Icons.Default.Save, contentDescription = "Сохранить")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // Сообщения об ошибках/успехе
            if (errorMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = "Ошибка",
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage!!,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (successMessage != null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Успех",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = successMessage!!,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            // Форма редактирования
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Основная информация",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Имя
                    OutlinedTextField(
                        value = firstName,
                        onValueChange = {
                            firstName = it
                            firstNameError = ""
                            errorMessage = null
                        },
                        label = { Text("Имя") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = firstNameError.isNotEmpty(),
                        supportingText = {
                            if (firstNameError.isNotEmpty()) {
                                Text(firstNameError)
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Фамилия
                    OutlinedTextField(
                        value = lastName,
                        onValueChange = {
                            lastName = it
                            lastNameError = ""
                            errorMessage = null
                        },
                        label = { Text("Фамилия") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = lastNameError.isNotEmpty(),
                        supportingText = {
                            if (lastNameError.isNotEmpty()) {
                                Text(lastNameError)
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Пол - выпадающий список
                    ExposedDropdownMenuBox(
                        expanded = genderExpanded,
                        onExpandedChange = { genderExpanded = !genderExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = gender,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("Пол") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded)
                            },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            isError = genderError.isNotEmpty(),
                            supportingText = {
                                if (genderError.isNotEmpty()) {
                                    Text(genderError)
                                }
                            },
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
                                        errorMessage = null
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Дата рождения
                    OutlinedButton(
                        onClick = { datePicker.show() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(),
                        enabled = !isLoading
                    ) {
                        Text(
                            if (birthDate.isEmpty()) "Выберите дату рождения"
                            else "Дата рождения: $birthDate"
                        )
                    }
                    if (birthDateError.isNotEmpty()) {
                        Text(
                            birthDateError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Телефон с маской
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            // Та же маска для телефона
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
                                errorMessage = null
                            }
                        },
                        label = { Text("Телефон") },
                        placeholder = { Text("+7 (xxx) xxx-xx-xx") },
                        singleLine = true,
                        isError = phoneError.isNotEmpty(),
                        supportingText = {
                            if (phoneError.isNotEmpty()) {
                                Text(phoneError)
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Phone,
                            imeAction = ImeAction.Done
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}