package com.ark.socialevent.ui.screens.profile

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.ark.socialevent.network.UserProfile
import com.ark.socialevent.network.UserRepository
import androidx.compose.foundation.text.KeyboardOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    userRepository: UserRepository,
    currentProfile: UserProfile?,
    onBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    var firstName by remember { mutableStateOf(currentProfile?.firstName ?: "") }
    var lastName by remember { mutableStateOf(currentProfile?.lastName ?: "") }
    var gender by remember { mutableStateOf(currentProfile?.gender ?: "") }
    var age by remember { mutableStateOf(currentProfile?.age?.toString() ?: "") }
    var phone by remember { mutableStateOf(currentProfile?.phone ?: "") }

    // Ошибки по полям как в RegisterScreen
    var firstNameError by remember { mutableStateOf("") }
    var lastNameError by remember { mutableStateOf("") }
    var genderError by remember { mutableStateOf("") }
    var ageError by remember { mutableStateOf("") }
    var phoneError by remember { mutableStateOf("") }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // Выпадающий список для пола
    var genderExpanded by remember { mutableStateOf(false) }
    val genders = listOf("Мужской", "Женский")

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
                                ageError = ""
                                phoneError = ""
                                errorMessage = null

                                // Проверки как в RegisterScreen
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
                                if (age.isBlank() || age.toIntOrNull() == null) {
                                    ageError = "Введите корректный возраст"
                                    hasError = true
                                } else {
                                    val ageInt = age.toInt()
                                    if (ageInt < 14) {
                                        ageError = "Возраст должен быть не менее 14 лет"
                                        hasError = true
                                    }
                                    if (ageInt > 100) {
                                        ageError = "Возраст должен быть не более 100 лет"
                                        hasError = true
                                    }
                                }

                                // Проверка формата телефона как в RegisterScreen
                                val phoneRegex = Regex("""\+7 \(\d{3}\) \d{3}-\d{2}-\d{2}""")
                                if (!phoneRegex.matches(phone)) {
                                    phoneError = "Введите корректный номер телефона в формате +7 (xxx) xxx-xx-xx"
                                    hasError = true
                                }

                                if (!hasError) {
                                    isLoading = true
                                    successMessage = null

                                    // РЕАЛЬНЫЙ ВЫЗОВ API
                                    userRepository.updateProfile(
                                        firstName = firstName,
                                        lastName = lastName,
                                        gender = gender,
                                        age = age.toInt(),
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

                    // Пол - выпадающий список как в RegisterScreen
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

                    // Возраст
                    OutlinedTextField(
                        value = age,
                        onValueChange = { newAge ->
                            if (newAge.all { it.isDigit() } && newAge.length <= 3) {
                                age = newAge
                                ageError = ""
                                errorMessage = null
                            }
                        },
                        label = { Text("Возраст") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = ageError.isNotEmpty(),
                        supportingText = {
                            if (ageError.isNotEmpty()) {
                                Text(ageError)
                            }
                        },
                        keyboardOptions = KeyboardOptions.Default.copy(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Next
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Телефон с маской как в RegisterScreen
                    OutlinedTextField(
                        value = phone,
                        onValueChange = {
                            // Та же маска для телефона что и в RegisterScreen
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